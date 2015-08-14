package lsfusion.server.data;

import lsfusion.server.Settings;

import java.sql.SQLException;

public class SQLUniqueViolationException extends SQLHandledException {
    
    private final boolean possibleRaceCondition;

    public SQLUniqueViolationException(Boolean isInTransaction, boolean possibleRaceCondition) {
        super(isInTransaction);
        
        this.possibleRaceCondition = possibleRaceCondition;
    }
    
    public SQLUniqueViolationException raceCondition() {
        assert !possibleRaceCondition;
        
        return new SQLUniqueViolationException(isInTransaction, true);
    }

    @Override
    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) throws SQLException {
        if(attempts > Settings.get().getTooMuchAttempts())
            return false;

        return possibleRaceCondition;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String toString() {
        return "UNIQUE_VIOLATION" + (possibleRaceCondition ? " POS_RACE" : "") ;
    }
}
