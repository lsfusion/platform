package lsfusion.server.data;

import java.sql.SQLException;

public class SQLTooLargeQueryException extends SQLHandledException {

    private final long limit;
    private final long rowSize;

    public SQLTooLargeQueryException(long limit, long rowSize) {
        this.limit = limit;
        this.rowSize = rowSize;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner) throws SQLException {
        return false;
    }

    public String toString() {
        return "TOO LARGE QUERY LIMIT :" + limit + ", ROWSIZE :" + rowSize;
    }
}
