package lsfusion.server.data.sql.exception;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;

import java.sql.SQLException;

public class SQLTooLongQueryException extends SQLHandledException {
    
    private final int length;
    private final String select;

    public SQLTooLongQueryException(int length, String select) {
        this.length = length;
        this.select = select;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) throws SQLException {
        return false;
    }

    @Override
    public String getMessage() {
        return "TOO LONG QUERY ROWS, LENGTH : " + length + ", QUERY : " + select;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "ln";
    }
}
