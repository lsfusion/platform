package lsfusion.server.data;

import lsfusion.server.ServerLoggers;

import java.sql.Connection;
import java.sql.SQLException;

public class ExConnection {
    public Connection sql;
    public SQLTemporaryPool temporary;

    public ExConnection(Connection sql, SQLTemporaryPool temporary) {
        this.sql = sql;
        this.temporary = temporary;
    }
    
    public void close() throws SQLException {
        ServerLoggers.sqlLogger.info("CONNECTION CLOSE " + sql);
        sql.close();
    }
}
