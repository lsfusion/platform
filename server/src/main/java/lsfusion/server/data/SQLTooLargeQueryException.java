package lsfusion.server.data;

import java.sql.SQLException;

public class SQLTooLargeQueryException extends SQLHandledException {

    private final long rowCount;
    private final long limit;
    private final long rowSize;

    public SQLTooLargeQueryException(long rowCount, long limit, long rowSize) {
        this.rowCount = rowCount;
        this.limit = limit;
        this.rowSize = rowSize;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) throws SQLException {
        return false;
    }

    public String toString() {
        return "TOO LARGE QUERY ROWS " + rowCount + ", LIMIT :" + limit + ", ROWSIZE :" + rowSize;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "lr";
    }
}
