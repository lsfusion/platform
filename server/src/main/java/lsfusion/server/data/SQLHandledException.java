package lsfusion.server.data;

import java.sql.SQLException;

public abstract class SQLHandledException extends Exception{

    public boolean repeatApply(SQLSession sql) throws SQLException {
        return true;
    }
}
