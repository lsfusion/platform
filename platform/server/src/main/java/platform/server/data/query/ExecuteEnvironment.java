package platform.server.data.query;

import platform.base.TwinImmutableObject;
import platform.server.Settings;
import platform.server.caches.AbstractTranslateValues;
import platform.server.data.SQLSession;
import platform.server.data.translator.MapValuesTranslate;

import java.sql.Connection;
import java.sql.SQLException;

public class ExecuteEnvironment extends AbstractTranslateValues<ExecuteEnvironment> {

    public final static ExecuteEnvironment EMPTY = new ExecuteEnvironment();
    public final static ExecuteEnvironment NOREADONLY = new ExecuteEnvironment(true);

    private boolean noReadOnly;
    private boolean volatileStats;

    public ExecuteEnvironment() {
        this.noReadOnly = false;
        this.volatileStats = false;
    }

    public ExecuteEnvironment(boolean noReadOnly) {
        this.noReadOnly = noReadOnly;
        this.volatileStats = false;
    }

    void add(ExecuteEnvironment environment) {
        noReadOnly = noReadOnly || environment.noReadOnly;
        volatileStats = volatileStats || environment.volatileStats;
    }


    public void addNoReadOnly() {
        noReadOnly = true;
    }

    public void addVolatileStats() {
        volatileStats = true;
    }

    public void before(SQLSession sqlSession, Connection connection, String command) throws SQLException {
        if(noReadOnly)
            sqlSession.pushNoReadOnly(connection);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.pushVolatileStats(connection);
    }

    public void after(SQLSession sqlSession, Connection connection, String command) throws SQLException {
        if(noReadOnly)
            sqlSession.popNoReadOnly(connection);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.popVolatileStats(connection);
    }

    public ExecuteEnvironment translateValues(MapValuesTranslate translate) {
        return this;
    }

    public boolean twins(TwinImmutableObject o) {
        throw new RuntimeException("not supported yet");


    }

    public int immutableHashCode() {
        throw new RuntimeException("not supported yet");
    }
}
