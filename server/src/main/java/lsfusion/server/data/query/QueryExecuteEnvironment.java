package lsfusion.server.data.query;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;
import java.sql.Statement;

// Mutable !!! нужен Thread Safe
public abstract class QueryExecuteEnvironment {

    public abstract QueryExecuteInfo getInfo(SQLSession session, int transactTimeout);
    
    public void beforeConnection(SQLSession session, QueryExecuteInfo info) throws SQLException {
        if(info.timeout > 0) // из-за бага в драйвере postgresql
            session.lockNeedPrivate();
    }
    
    public void afterConnection(SQLSession session, OperationOwner owner, QueryExecuteInfo info) throws SQLException {
        if(info.timeout > 0)
            session.lockTryCommon(owner);
    }    
    
    public void beforeStatement(Statement statement, SQLSession session, QueryExecuteInfo info) throws SQLException {
        if(info.timeout > 0)
            statement.setQueryTimeout(info.timeout);
    }
    
    public final static QueryExecuteEnvironment DEFAULT = new QueryExecuteEnvironment() {
        public QueryExecuteInfo getInfo(SQLSession session, int transactTimeout) {
            if(session.isInTransaction() && transactTimeout > 0 && !session.isNoHandled() && !session.isNoTransactTimeout())
                return new QueryExecuteInfo(transactTimeout, true);
            return QueryExecuteInfo.EMPTY;
        }
    };  
}
