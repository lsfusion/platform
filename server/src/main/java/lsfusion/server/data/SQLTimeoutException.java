package lsfusion.server.data;

public class SQLTimeoutException extends SQLHandledException {
    
    public final boolean isTransactTimeout;

    public SQLTimeoutException(boolean isTransactTimeout) {
        this.isTransactTimeout = isTransactTimeout;
    }
}
