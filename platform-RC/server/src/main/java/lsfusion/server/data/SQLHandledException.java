package lsfusion.server.data;

import java.sql.SQLException;

public abstract class SQLHandledException extends Exception implements HandledException{

    public abstract String getDescription(boolean wholeTransaction);

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) throws SQLException {
        return true;
    }

    public boolean repeatCommand() {
        return false;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return true;
    }
}
