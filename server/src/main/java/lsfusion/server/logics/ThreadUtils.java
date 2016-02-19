package lsfusion.server.logics;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.context.Context;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.ExecutionContext;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadUtils {

    public static void interruptThread(Context context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getLogicsInstance().getDbManager(), thread);
    }

    public static void interruptThread(ExecutionContext context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getDbManager(), thread);
    }

    public static void interruptThread(DBManager dbManager, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(dbManager.getStopSql(), thread);
    }

    public static void interruptThread(SQLSession sqlSession, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            SQLSession.cancelExecutingStatement(sqlSession, thread.getId(), true);
            thread.interrupt();
        }
    }

    public static void cancelThread(Context context, Thread thread) throws SQLException, SQLHandledException {
        cancelThread(context.getLogicsInstance().getDbManager().getStopSql(), thread);
    }

    public static void cancelThread(SQLSession session, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null)
            SQLSession.cancelExecutingStatement(session, thread.getId(), false);
    }
    public static ThreadGroup getRootThreadGroup( ) {
        ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup ptg;
        while ( (ptg = tg.getParent( )) != null )
            tg = ptg;
        return tg;
    }
    public static ImSet<Thread> getAllThreads( ) {
        final ThreadGroup root = getRootThreadGroup( );
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
        int nAlloc = thbean.getThreadCount( );
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[ nAlloc ];
            n = root.enumerate( threads, true );
        } while ( n == nAlloc );
        return SetFact.toSet(java.util.Arrays.copyOf( threads, n ));
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
}