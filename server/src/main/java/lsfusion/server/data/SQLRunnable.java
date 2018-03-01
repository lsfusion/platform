package lsfusion.server.data;

import java.sql.SQLException;

public interface SQLRunnable {
    void run() throws SQLException, SQLHandledException;
}
