package lsfusion.server.data;

public class SQLConflictException extends SQLHandledException {

    private final boolean updateConflict;
    public SQLConflictException(boolean updateConflict, boolean isInTransaction) {
        super(isInTransaction);
        
        this.updateConflict = updateConflict;
    }

    public String toString() {
        return updateConflict ? "UPDATE_CONFLICT" : "DEAD_LOCK"; 
    }
}
