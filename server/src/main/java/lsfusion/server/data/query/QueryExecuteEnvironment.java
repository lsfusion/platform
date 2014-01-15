package lsfusion.server.data.query;

import lsfusion.server.data.SQLSession;

import java.sql.SQLException;
import java.sql.Statement;

// Mutable !!! нужен Thread Safe
public interface QueryExecuteEnvironment {

    boolean beforeStatement(Statement statement, SQLSession session, int transactTimeout) throws SQLException;
    
    public final static QueryExecuteEnvironment DEFAULT = new QueryExecuteEnvironment() {
        public boolean beforeStatement(Statement statement, SQLSession session, int transactTimeout) throws SQLException {
            if(session.isInTransaction() && transactTimeout > 0 && !session.isNoHandled() && !session.isNoTransactTimeout()) {
                statement.setQueryTimeout(transactTimeout);
                return true;
            }
            return false;
        }
    };  
}
