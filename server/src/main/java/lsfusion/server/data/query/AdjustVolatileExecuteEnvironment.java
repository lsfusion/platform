package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.server.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SQLTimeoutException;

import java.sql.SQLException;

public class AdjustVolatileExecuteEnvironment extends QueryExecuteEnvironment {

    private boolean volatileStats;
    private boolean fixVolatile;
    private int timeout = Settings.get().getTimeoutStart();

    private static int getTransAdjust(SQLSession sqlSession) {
        return sqlSession.getSecondsFromTransactStart();
    }
            
    public synchronized AdjustState before(SQLSession sqlSession) throws SQLException {
        if(sqlSession.isVolatileStats() || sqlSession.isNoHandled())
            return null;
        
        if(volatileStats)
            sqlSession.pushVolatileStats(null);

        return new AdjustState(timeout, volatileStats, getTransAdjust(sqlSession));
    }

    public void after(AdjustState queryExecState, SQLSession sqlSession) throws SQLException {
        if(queryExecState.volatileStats)
            sqlSession.popVolatileStats(null);
    }

    public void succeeded(AdjustState state) {
        if(state.volatileStats && timeout > state.transAdjust) { // только если больше чем transAdjust, потому как без volatileStats с большой вероятностью выполнялось для меньшего timeout'а
            fixVolatile = true; // помечаем запрос как опасный, и всегда будем использовать volatile stats
        }
    }

    public synchronized void failed(AdjustState state, SQLHandledException e) {
        
        if(!(e instanceof SQLTimeoutException && !((SQLTimeoutException)e).isTransactTimeout))
            return;
        
        // discard'м если состояние на конец отличается от состояния на начало
        if(!(volatileStats == state.volatileStats && timeout == state.prevTimeout))
            return;

        int degree = Settings.get().getTimeoutDegree();
        if(fixVolatile) {
            volatileStats = true; // на всякий случай, так как suceeded не synchronized 
            timeout = 0; // timeout не нужен
        } else {
            if(timeout < state.transAdjust)
                timeout = state.transAdjust;
            else if(volatileStats)
                timeout *= degree;
            volatileStats = !volatileStats;
        }
    }

    public synchronized QueryExecuteInfo getInfo(SQLSession session, int transactTimeout) {
        int setTimeout = timeout;
        if(setTimeout > 0)
            setTimeout = BaseUtils.max(setTimeout, getTransAdjust(session));
        
        boolean result = false;
        if(session.isInTransaction() && !session.isNoTransactTimeout() && transactTimeout > 0 && (setTimeout >= transactTimeout || setTimeout == 0)) {
            setTimeout = transactTimeout;
            result = volatileStats;
        }
        return new QueryExecuteInfo(setTimeout, result);
    }
}
