package lsfusion.server.data.query;

import lsfusion.server.Settings;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;
import java.sql.Statement;

public class AdjustVolatileExecuteEnvironment implements QueryExecuteEnvironment {

    private boolean volatileStats;
    private boolean fixVolatile;
    private int timeout = Settings.get().getTimeoutStart();

    private static class State {
        public final int prevTimeout;

        private State(int prevTimeout) {
            this.prevTimeout = prevTimeout;
        }
    }


    public synchronized Object before(SQLSession sqlSession) throws SQLException {
        if(sqlSession.isVolatileStats() || sqlSession.isNoHandled())
            return null;
        
        if(volatileStats) {
            sqlSession.pushVolatileStats(null);
            return new State(timeout);
        }

        return timeout;
    }

    public void after(Object queryExecState, SQLSession sqlSession) throws SQLException {
        if(queryExecState instanceof State)
            sqlSession.popVolatileStats(null);
    }

    public void succeeded(Object state) {
        if(state instanceof State) {
            fixVolatile = true; // помечаем запрос как опасный, и всегда будем использовать volatile stats
        }
    }

    public synchronized void failed(Object state) {
        // discard'м если состояние на конец отличается от состояния на начало
        if(volatileStats) {
            if(!(state instanceof State && ((State)state).prevTimeout==timeout))
                return;
        } else {
            if(!(state instanceof Integer && timeout == ((Integer) state)))
                return;
        }

        int degree = Settings.get().getTimeoutDegree();
        if(fixVolatile) {
            volatileStats = true; // на всякий случай, так как suceeded не synchronized 
            timeout *= degree;
        } else {
            if(volatileStats)
                timeout *= degree;
            volatileStats = !volatileStats;
        }
    }

    public boolean beforeStatement(Statement statement, SQLSession session, int transactTimeout) throws SQLException {
        assert !session.isNoHandled();

        int setTimeout = timeout;
        boolean result = false;
        if(session.isInTransaction() && !session.isNoTransactTimeout() && transactTimeout > 0 && timeout >= transactTimeout) {
            setTimeout = transactTimeout;
            result = volatileStats;
        }            
        statement.setQueryTimeout(setTimeout);
        return result;
    }
}
