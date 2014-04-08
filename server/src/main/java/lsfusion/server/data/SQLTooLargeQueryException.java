package lsfusion.server.data;

import java.sql.SQLException;

public class SQLTooLargeQueryException extends SQLHandledException {

    public SQLTooLargeQueryException() {
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return false;
    }

    public String toString() {
        return "TOO LARGE QUERY";
    }
}
