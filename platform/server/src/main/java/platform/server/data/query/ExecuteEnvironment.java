package platform.server.data.query;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.Settings;
import platform.server.caches.AbstractTranslateValues;
import platform.server.data.SQLSession;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;

import java.sql.Connection;
import java.sql.SQLException;

public class ExecuteEnvironment extends AbstractTranslateValues<ExecuteEnvironment> implements TypeEnvironment {

    public final static ExecuteEnvironment EMPTY = new ExecuteEnvironment();
    public final static ExecuteEnvironment NOREADONLY = new ExecuteEnvironment(true);

    private boolean noReadOnly;
    private boolean volatileStats;
    private boolean noPrepare;

    private ImSet<ImList<Type>> recursions;
    private ImSet<ImList<Type>> concTypes;

    public ExecuteEnvironment() {
        this(false);
    }

    public ExecuteEnvironment(boolean noReadOnly) {
        this.noReadOnly = noReadOnly;
        this.volatileStats = false;
        this.noPrepare = false;

        recursions = SetFact.EMPTY();
        concTypes = SetFact.EMPTY();
    }

    public void add(ExecuteEnvironment environment) {
        noReadOnly = noReadOnly || environment.noReadOnly;
        volatileStats = volatileStats || environment.volatileStats;
        noPrepare = noPrepare || environment.noPrepare;

        recursions = recursions.merge(environment.recursions);
        concTypes = concTypes.merge(environment.concTypes);
    }

    public void addNoReadOnly() {
        noReadOnly = true;
    }

    public void addVolatileStats() {
        volatileStats = true;
    }

    public void addNoPrepare() {
        noPrepare = true;
    }

    public void addNeedRecursion(ImList<Type> types) {
        recursions = recursions.merge(types);
    }

    public void addNeedType(ImList<Type> types) {
        concTypes = concTypes.merge(types);
    }

    public void before(SQLSession sqlSession, Connection connection, String command) throws SQLException {
        for(ImList<Type> concType : concTypes)
            sqlSession.typePool.ensureConcType(concType);
        for(ImList<Type> recursion : recursions)
            sqlSession.typePool.ensureRecursion(recursion);

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

    public boolean isNoPrepare() {
        return noPrepare;
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
