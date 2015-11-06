package lsfusion.server.data;

public class SQLConflictException extends SQLHandledException {

    private final boolean updateConflict;
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
