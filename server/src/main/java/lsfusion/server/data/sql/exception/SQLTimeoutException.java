package lsfusion.server.data.sql.exception;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;

import java.sql.SQLException;

public class SQLTimeoutException extends SQLHandledException {
    public enum Type { INTERRUPT, CANCEL, TIMEOUT, TRANSACTTIMEOUT }

    private final Boolean isTransactTimeout;
    public final Type type;

    public SQLTimeoutException(Boolean isTransactTimeout, Boolean isForcedCancel) {
        this.isTransactTimeout = isTransactTimeout;
        this.type = isForcedCancel != null ? (isForcedCancel ? Type.INTERRUPT : Type.CANCEL) :
                                                isTransactTimeout ? Type.TRANSACTTIMEOUT : Type.TIMEOUT;
    }

    @Override
    public String getMessage() {
        return "TIMEOUT" + (isTransactTimeout ? " MAX" : "") + " " + type;
    }

    public static String ADJUSTTRANSTIMEOUT = "tt";

    @Override
    public String getDescription(boolean wholeTransaction) {
        return isTransactTimeout ? "ut" : (wholeTransaction ? ADJUSTTRANSTIMEOUT : "st");
    }

    @Override
    public boolean repeatCommand() {
        return type == Type.TIMEOUT;
    }

    @Override
    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) {
        return type == Type.TRANSACTTIMEOUT || type == Type.TIMEOUT;
    }

    public boolean isTransactTimeout() {
        return type == Type.TRANSACTTIMEOUT;
    }

    public boolean isCancel() {
        return type == Type.CANCEL;
    }
}
