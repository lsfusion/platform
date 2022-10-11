package lsfusion.server.data.sql.connection;

import lsfusion.base.BaseUtils;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.sql.table.SQLTemporaryPool;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ExConnection {
    public Connection sql;
    public SQLTemporaryPool temporary;

    public ExConnection(Connection sql, SQLTemporaryPool temporary) {
        this.sql = sql;
        this.temporary = temporary;
        long currentTime = System.currentTimeMillis();
        this.timeStarted = currentTime;
        this.lastTempTablesActivity = currentTime;
    }

    public void restartConnection(Connection newConnection, SQLSessionContextProvider contextProvider) throws SQLException {
        this.sql = newConnection;

        updateContext(contextProvider);
    }

    private String timeZone;
    public void updateContext(SQLSessionContextProvider contextProvider) throws SQLException {
        LocalePreferences localePreferences = contextProvider.getLocalePreferences();

        String newTimeZone = localePreferences != null ? ("'" + localePreferences.timeZone + "'") : "DEFAULT";
        if (timeZone == null || !timeZone.equals(newTimeZone)) {
            timeZone = newTimeZone;

            Statement statement = SQLSession.createSingleStatement(sql);
            try {
                statement.execute("SET TIMEZONE=" + timeZone);
            } catch (SQLException e) {
                ServerLoggers.sqlLogger.error(statement.toString());
                throw e;
            } finally {
                statement.close();
            }
        }
        // timezone (SET TIME ZONE), dateformat, timeformat (??), locale (SET LC_ALL or specific depending on what will happens with index) should be supported
    }
    
    private Integer lastLogLevel; // оптимизация
    public synchronized void updateLogLevel(SQLSyntax syntax) {
        int logLevel = Settings.get().getLogLevelJDBC();
        if(lastLogLevel == null || !lastLogLevel.equals(logLevel)) {
            syntax.setLogLevel(logLevel);
            lastLogLevel = logLevel;
        }
    }

    public void checkClosed() throws SQLException {
        ServerLoggers.assertLog(!sql.isClosed(), "CONNECTION IS ALREADY CLOSED " + sql);
    }

    public double lengthScore;
    public double timeScore;
    public long timeStarted;
    public int maxTotalSessionTablesCount;
    public long lastTempTablesActivity;

    public void registerExecute(int length, long runTime) {
        Settings settings = Settings.get();
        int degree = settings.getQueryExecuteDegree();
        // тут по хорошему надо было бы использовать DoubleAdder но он только в 8-й java
        lengthScore += BaseUtils.pow(((double)length/((double)settings.getQueryLengthAverageMax())), degree);
        timeScore += BaseUtils.pow(((double)runTime/((double)settings.getQueryTimeAverageMax())), degree);
    }
}
