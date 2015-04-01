package lsfusion.server.data.query;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLQuery;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;
import java.sql.Statement;

public class DynamicExecEnvSnapshot {
    
    public final int timeout;
    public final boolean isTransactTimeout;
    public final boolean isUserVolatileStats;

    public static final DynamicExecEnvSnapshot EMPTY = new DynamicExecEnvSnapshot(0, false);

    public DynamicExecEnvSnapshot(int seconds, boolean isTransact) {
        this(seconds, isTransact, false);
    }

    public DynamicExecEnvSnapshot(int seconds, boolean isTransact, boolean isUserVolatileStats) {
        this.timeout = seconds;
        this.isTransactTimeout = isTransact;
        this.isUserVolatileStats = isUserVolatileStats;
    }

    public void afterConnection(SQLSession session, OperationOwner owner) throws SQLException {
        if(isUserVolatileStats)
            session.popVolatileStats(owner);
        if(timeout > 0)
            session.lockTryCommon(owner);
    }

    public void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException {
        if(timeout > 0) // из-за бага в драйвере postgresql
            session.lockNeedPrivate();
        if(isUserVolatileStats)
            session.pushVolatileStats(owner);
    }

    public DynamicExecEnvSnapshot withVolatileStats() {
        assert !isUserVolatileStats;
        return new DynamicExecEnvSnapshot(timeout, isTransactTimeout, true);
    }
    
    public boolean needTimeoutLock() {
        return timeout > 0;
    }

    public void beforeStatement(Statement statement, SQLSession session) throws SQLException {
        if(timeout > 0)
            statement.setQueryTimeout(timeout);
    }

    public ImMap<SQLQuery, String> getMaterializedQueries() {
        return MapFact.EMPTY();
    }
}
