package lsfusion.server.data;

import java.sql.SQLException;

public abstract class SQLHandledException extends Exception{

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return true;
    }
}
