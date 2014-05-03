package lsfusion.server.data;

import java.sql.SQLException;

public abstract class SQLHandledException extends Exception{
    
    private Boolean isInTransaction;
    
    public boolean isInTransaction() {
        return isInTransaction;
    }

    protected SQLHandledException(Boolean isInTransaction) {
        this.isInTransaction = isInTransaction;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return true;
    }
}
