package lsfusion.server.data;

import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.sql.SQLSyntax;

import java.sql.Connection;
import java.sql.SQLException;

public class ExConnection {
    public Connection sql;
    public SQLTemporaryPool temporary;

    public ExConnection(Connection sql, SQLTemporaryPool temporary) {
        this.sql = sql;
        this.temporary = temporary;
    }
    
    private Integer lastLogLevel; // оптимизация
    public synchronized void updateLogLevel(SQLSyntax syntax) {
        int logLevel = Settings.get().getLogLevelJDBC();
        if(lastLogLevel == null || !lastLogLevel.equals(logLevel)) {
            syntax.setLogLevel(sql, logLevel);
            lastLogLevel = logLevel;
        }
    }
    
    public void close() throws SQLException {
        ServerLoggers.sqlLogger.info("CONNECTION CLOSE " + sql);
        sql.close();
    }
    
    public void checkClosed() throws SQLException {
        ServerLoggers.assertLog(!sql.isClosed(), "CONNECTION IS ALREADY CLOSED " + sql);
    }
}
