package lsfusion.server.logics;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ThreadUtils {

    public static void interruptThread(ExecutionContext context, Thread thread) throws SQLException, SQLHandledException {
        interruptThread(context.getDbManager(), context.getSession().sql, thread);
    }

    public static void interruptThread(DBManager dbManager, SQLSession sql, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            thread.interrupt();
            SQLSession.cancelExecutingStatement(dbManager, sql, thread.getId());
        }
    }
}