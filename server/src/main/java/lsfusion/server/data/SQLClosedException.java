package lsfusion.server.data;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLClosedException extends SQLHandledException {

    public transient final Connection connection;
    public transient final SQLException wrapped;     
    
    public SQLClosedException(Connection connection, SQLException wrapped) {
        this.connection = connection;
        this.wrapped = wrapped;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return sql.tryRestore(owner, connection);
    }

    public String toString() {
        return "CONNECTION_CLOSED " + connection + " " + wrapped;
    }
}
