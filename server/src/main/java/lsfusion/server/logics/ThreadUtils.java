package lsfusion.server.logics;

import lsfusion.server.context.Context;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

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
            thread.interrupt();
            SQLSession.cancelExecutingStatement(sqlSession, thread.getId());
        }
    }
}