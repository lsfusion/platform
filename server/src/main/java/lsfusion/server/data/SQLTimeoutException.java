package lsfusion.server.data;

public class SQLTimeoutException extends SQLHandledException {
    
    public final boolean isTransactTimeout;

    public SQLTimeoutException(boolean isTransactTimeout) {
        this.isTransactTimeout = isTransactTimeout;
    }

    public String toString() {
        return "TIMEOUT" + (isTransactTimeout ? " MAX" : "");
    }
}
