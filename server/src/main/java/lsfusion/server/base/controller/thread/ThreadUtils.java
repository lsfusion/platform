package lsfusion.server.base.controller.thread;

import lsfusion.base.SystemUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class ThreadUtils {

    public static void interruptThread(Context context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getLogicsInstance().getDbManager(), thread);
    }

    public static void interruptThread(ExecutionContext context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getDbManager(), thread);
    }

    public static void interruptThread(DBManager dbManager, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            ServerLoggers.exinfoLog("THREAD INTERRUPT " + thread);
            thread.interrupt(); // it's better to do it before to prevent sql query execution
            SQLSession.cancelExecutingStatement(dbManager, thread, true);
        }
    }

    public static void sleep(long millis) {
        SystemUtils.sleep(millis);
    }

    public static void interruptThread(DBManager dbManager, Thread thread, Future future) throws SQLException, SQLHandledException {
        if(thread != null)
            SQLSession.cancelExecutingStatement(dbManager, thread, true);
        future.cancel(true);
    }

    public static void cancelThread(Context context, Thread thread) throws SQLException, SQLHandledException {
        cancelThread(context.getLogicsInstance().getDbManager(), thread);
    }

    public static void cancelThread(DBManager dbManager, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null)
            SQLSession.cancelExecutingStatement(dbManager, thread, false);
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
                && (ignoreSocketRead || !stackTrace.startsWith("java.net.SocketInputStream.socketRead0"))
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