package lsfusion.server.data.sql.exception;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.Settings;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLClosedException extends SQLHandledException {

    public transient final Connection connection;
    public transient final SQLException wrapped;
    public final boolean isPrivate;
    private final boolean isInTransaction;

    public boolean isInTransaction() {
        return isInTransaction;
    }
    
    public SQLClosedException(Connection connection, boolean isInTransaction, SQLException wrapped, boolean isPrivate) {
        this.connection = connection;
        this.wrapped = wrapped;
        this.isPrivate = isPrivate;
        this.isInTransaction = isInTransaction;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) {
        return false;
    }

    @Override
    public String getMessage() {
        return "CONNECTION_CLOSED " + connection + " " + wrapped + " " + isPrivate;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "cl";
    }
}
