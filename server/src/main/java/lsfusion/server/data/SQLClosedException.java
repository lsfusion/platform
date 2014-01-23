package lsfusion.server.data;

import java.sql.SQLException;

public class SQLClosedException extends SQLHandledException {

    public SQLClosedException() {
    }

    public boolean repeatApply(SQLSession sql) throws SQLException {
        return sql.tryRestore();
    }

    public String toString() {
        return "CONNECTION_CLOSED";
    }
}
