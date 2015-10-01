package lsfusion.server.data;

import java.sql.SQLException;

public abstract class SQLHandledException extends Exception implements HandledException{

    public abstract String getDescription();

    protected Boolean isInTransaction;
    
    public boolean isInTransaction() {
        return isInTransaction;
    }

    protected SQLHandledException(Boolean isInTransaction) {
        this.isInTransaction = isInTransaction;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) throws SQLException {
        return true;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return true;
    }
}
