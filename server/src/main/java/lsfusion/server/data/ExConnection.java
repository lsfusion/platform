package lsfusion.server.data;

import lsfusion.base.BaseUtils;
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
        this.timeStarted = System.currentTimeMillis();
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

    public double lengthScore;
    public double timeScore;
    public long timeStarted;
    public int maxTotalSessionTablesCount;

    public void registerExecute(int length, long runTime) {
        Settings settings = Settings.get();
        int degree = settings.getQueryExecuteDegree();
        // тут по хорошему надо было бы использовать DoubleAdder но он только в 8-й java
        lengthScore += BaseUtils.pow(((double)length/((double)settings.getQueryLengthAverageMax())), degree);
        timeScore += BaseUtils.pow(((double)runTime/((double)settings.getQueryTimeAverageMax())), degree);
    }
}
