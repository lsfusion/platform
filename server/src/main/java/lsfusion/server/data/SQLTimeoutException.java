package lsfusion.server.data;

public class SQLTimeoutException extends SQLHandledException {
    
    public final boolean isTransactTimeout;

    public SQLTimeoutException(boolean isTransactTimeout) {
        this.isTransactTimeout = isTransactTimeout;
    }

    public String toString() {
        return "TIMEOUT" + (isTransactTimeout ? " MAX" : "");
    }

    public static String ADJUSTTRANSTIMEOUT = "tt";

    @Override
    public String getDescription(boolean wholeTransaction) {
        return isTransactTimeout ? "ut" : (wholeTransaction ? ADJUSTTRANSTIMEOUT : "st");
    }

    @Override
    public boolean repeatCommand() {
        return !isTransactTimeout;
    }
}
