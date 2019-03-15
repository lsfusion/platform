package lsfusion.server.data.sql.lambda;

import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

public interface SQLRunnable {
    void run() throws SQLException, SQLHandledException;
}
