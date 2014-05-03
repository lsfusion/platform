package lsfusion.server.data;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLClosedException extends SQLHandledException {

    public transient final Connection connection;
    public transient final SQLException wrapped;
    public final boolean isPrivate;
    
    public SQLClosedException(Connection connection, boolean isInTransaction, SQLException wrapped, boolean isPrivate) {
        super(isInTransaction);
        
        this.connection = connection;
        this.wrapped = wrapped;
        this.isPrivate = isPrivate;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return sql.tryRestore(owner, connection, isPrivate);
    }

    public String toString() {
        return "CONNECTION_CLOSED " + connection + " " + wrapped + " " + isPrivate;
    }
}
