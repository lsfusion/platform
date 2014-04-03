package lsfusion.server.data;

import java.sql.SQLException;

public class SQLClosedException extends SQLHandledException {

    private final String sqlString;
    
    public SQLClosedException(String sqlString) {
        this.sqlString = sqlString;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return sql.tryRestore(owner);
    }

    public String toString() {
        return "CONNECTION_CLOSED " + sqlString;
    }
}
