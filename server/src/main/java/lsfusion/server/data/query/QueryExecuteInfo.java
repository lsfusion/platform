package lsfusion.server.data.query;

public class QueryExecuteInfo {
    
    public final int timeout;
    public final boolean isTransactTimeout;
    public final boolean isUserVolatileStats;

    public static final QueryExecuteInfo EMPTY = new QueryExecuteInfo(0, false);

    public QueryExecuteInfo(int seconds, boolean isTransact) {
        this(seconds, isTransact, false);
    }

    public QueryExecuteInfo(int seconds, boolean isTransact, boolean isUserVolatileStats) {
        this.timeout = seconds;
        this.isTransactTimeout = isTransact;
        this.isUserVolatileStats = isUserVolatileStats;
    }

    public QueryExecuteInfo withVolatileStats() {
        assert !isUserVolatileStats;
        return new QueryExecuteInfo(timeout, isTransactTimeout, true);
    }
    
    public boolean needTimeoutLock() {
        return timeout > 0;
    }
}
