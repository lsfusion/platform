package lsfusion.server.logics;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ThreadUtils {

    public static void interruptThread(ExecutionContext context, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            thread.interrupt();
            SQLSession.cancelExecutingStatement(context, thread.getId());
        }
    }
}