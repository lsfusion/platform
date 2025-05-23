package lsfusion.server.base.controller.thread;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadUtils {

    public static void interruptThreadExecutor(ExecutorService executor, ExecutionContext context) {
        if(executor != null) {
            try {
                Field workerField = ThreadPoolExecutor.class.getDeclaredField("workers");
                workerField.setAccessible(true);
                Class workerClass = Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker");

                HashSet<Object> workers = (HashSet<Object>) workerField.get(executor);
                Field threadField = workerClass.getDeclaredField("thread");
                threadField.setAccessible(true);
                for (Object worker : workers) {
                    interruptThread(context, (Thread) threadField.get(worker));
                }
            } catch (Throwable e) {
                ServerLoggers.systemLogger.error("Failed to kill sql processes in TaskRunner", e);
            }

            executor.shutdownNow();
        }
    }

    public static void interruptThread(Context context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getLogicsInstance().getDbManager(), thread);
    }

    public static void interruptThread(ExecutionContext context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getDbManager(), thread);
    }

    public static ConcurrentWeakHashMap<Thread, ThrowableWithStack> interruptThread = new ConcurrentWeakHashMap<>();

    public static boolean interruptThread(DBManager dbManager, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            ThrowableWithStack interrupt = new ThrowableWithStack(new InterruptedException());

            ServerLoggers.exinfoLog("THREAD INTERRUPT " + thread);
            interruptThread.put(thread, interrupt);
            thread.interrupt(); // it's better to do it before to prevent sql query execution
            ServerLoggers.exinfoLog("THREAD INTERRUPT ENDED " + thread);

            return interruptSQL(dbManager, thread, interrupt);
        }
        return false;
    }

    public static void sleep(long millis) {
        SystemUtils.sleep(millis);
    }

    public static boolean interruptSQL(DBManager dbManager, Thread thread, ThrowableWithStack interrupt) throws SQLException, SQLHandledException {
        return SQLSession.cancelExecutingStatement(dbManager, thread, interrupt);
    }

    public static void checkThreadInterrupted() {
        if(Thread.interrupted()) {
            Thread currentThread = Thread.currentThread();
            currentThread.interrupt();
            throw Throwables.propagate(getThreadInterrupt(interruptThread.remove(currentThread)));
        }
    }

    public static Exception getThreadInterrupt(ThrowableWithStack reason) {
        Exception exception = new InterruptedException(); // we want to keep interrupted as a root cause
        if(reason != null)
            exception = new NestedThreadException(exception, new ThrowableWithStack[]{reason});
        return exception;
    }

    public static ThreadGroup getRootThreadGroup( ) {
        ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup ptg;
        while ( (ptg = tg.getParent( )) != null )
            tg = ptg;
        return tg;
    }
    public static ImSet<Thread> getAllThreads( ) {
        if(Settings.get().isUseSafeMonitorProcess()) {
            return SetFact.fromJavaSet(Thread.getAllStackTraces().keySet());
        } else {
            final ThreadGroup root = getRootThreadGroup();
            final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
            int nAlloc = thbean.getThreadCount();
            int n;
            Thread[] threads;
            do {
                nAlloc *= 2;
                threads = new Thread[nAlloc];
                n = root.enumerate(threads, true);
            } while (n == nAlloc);
            return SetFact.toSet(java.util.Arrays.copyOf(threads, n));
        }
    }

//    есть подозрение что от такой реализации крэшится JVM
//    public static ImSet<Thread> getAllThreads() {
//        return SetFact.toSet((Thread[]) ReflectionUtils.invokePrivateMethod(Thread.class, null, "getThreads", new Class[]{}));
//    }

    public static Thread getThreadById(long id) {
        for(Thread thread : getAllThreads())
            if(thread.getId() == id)
                return thread;
        return null;
    }

    public static Map<Long, Thread> getThreadMap() {
        Map<Long, Thread> result = new HashMap<>();
        for(Thread thread : getAllThreads())
            result.put(thread.getId(), thread);
        return result;
    }

    public static boolean isActiveJavaProcess(ThreadInfo threadInfo) {
        String status = threadInfo == null ? null : String.valueOf(threadInfo.getThreadState());
        String stackTrace = threadInfo == null ? null : getJavaStack(threadInfo.getStackTrace());
        return isActiveJavaProcess(status, stackTrace, true);
    }

    public static boolean isActiveJavaProcess(String status, String stackTrace, boolean ignoreSocketRead) {
        return status != null && (status.equals("RUNNABLE") || status.equals("BLOCKED")) && (stackTrace != null
                && !stackTrace.startsWith("java.net.DualStackPlainSocketImpl")
                && !stackTrace.startsWith("sun.awt.windows.WToolkit.eventLoop")
                && (ignoreSocketRead || !stackTrace.contains("java.net.SocketInputStream.socketRead0"))
                && !stackTrace.contains("sun.nio.ch.Net.poll") //java 17
                && !stackTrace.contains("sun.nio.ch.WEPoll.wait") //java 21
                && !stackTrace.startsWith("sun.management.ThreadImpl.dumpThreads0")
                && !stackTrace.startsWith("java.net.SocketOutputStream.socketWrite")
                && !stackTrace.startsWith("java.net.PlainSocketImpl")
                && !stackTrace.startsWith("java.io.FileInputStream.readBytes")
                && !stackTrace.startsWith("java.lang.UNIXProcess.waitForProcessExit"))
                && !stackTrace.contains("UpdateProcessMonitor");
    }

    public static String getJavaStack(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        String result = sb.toString();
        return result.isEmpty() ? null : result;
    }

    private static Set<Thread> finallyModeThreadSet = new HashSet<>();

    public static void setFinallyMode(Thread thread, boolean enable) {
        if(enable)
            finallyModeThreadSet.add(thread);
        else
            finallyModeThreadSet.remove(thread);
    }

    public static boolean isFinallyMode(Thread thread) {
        return finallyModeThreadSet.contains(thread);
    }
}