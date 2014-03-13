package lsfusion.server.data.query;

public class QueryExecuteInfo {
    
    public final int timeout;
    public final boolean isTransactTimeout;

    public static final QueryExecuteInfo EMPTY = new QueryExecuteInfo(0, false);
    
    public QueryExecuteInfo(int seconds, boolean isTransact) {
        this.timeout = seconds;
        this.isTransactTimeout = isTransact;
    }
    
    public boolean needTimeoutLock() {
        return timeout > 0;
    }
}
