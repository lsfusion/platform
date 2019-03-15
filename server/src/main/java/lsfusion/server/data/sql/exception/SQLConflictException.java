package lsfusion.server.data.sql.exception;

public class SQLConflictException extends SQLHandledException {

    public final boolean updateConflict;
    public SQLConflictException(boolean updateConflict) {
        this.updateConflict = updateConflict;
    }

    public String toString() {
        return updateConflict ? "UPDATE_CONFLICT" : "DEAD_LOCK"; 
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return updateConflict ? "cn" : "dd";
    }
}
