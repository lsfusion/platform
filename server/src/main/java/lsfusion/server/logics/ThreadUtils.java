package lsfusion.server.logics;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;

public class ThreadUtils {

    public static void interruptThread(SQLSession sqlSession, Thread thread) throws SQLException, SQLHandledException {
        if(thread != null) {
            thread.interrupt();
            SQLSession.cancelExecutingStatement(sqlSession, thread.getId());
        }
    }
}