package platform.server.data.query;

import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractTranslateValues;
import platform.server.data.SQLSession;
import platform.server.data.translator.MapValuesTranslate;

import java.sql.Connection;
import java.sql.SQLException;

public class ExecuteEnvironment extends AbstractTranslateValues<ExecuteEnvironment> {

    public final static ExecuteEnvironment EMPTY = new ExecuteEnvironment();
    public final static ExecuteEnvironment NOREADONLY = new ExecuteEnvironment(true);

    private boolean noReadOnly;

    public ExecuteEnvironment() {
        this.noReadOnly = false;
    }

    public ExecuteEnvironment(boolean noReadOnly) {
        this.noReadOnly = noReadOnly;
    }

    void add(ExecuteEnvironment environment) {
        noReadOnly = noReadOnly || environment.noReadOnly;
    }


    public void addNoReadOnly() {
        noReadOnly = true;
    }

    public void before(SQLSession sqlSession, Connection connection) throws SQLException {
        if(noReadOnly)
            sqlSession.pushNoReadOnly(connection);
    }

    public void after(SQLSession sqlSession, Connection connection) throws SQLException {
        if(noReadOnly)
            sqlSession.popNoReadOnly(connection);
    }

    public ExecuteEnvironment translateValues(MapValuesTranslate translate) {
        return this;
    }

    public boolean twins(TwinImmutableInterface o) {
        throw new RuntimeException("not supported yet");


    }

    public int immutableHashCode() {
        throw new RuntimeException("not supported yet");
    }
}
