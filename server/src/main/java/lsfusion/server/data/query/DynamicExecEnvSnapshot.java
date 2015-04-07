package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.ExConnection;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLQuery;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;
import java.sql.Statement;

// ThreadSafe
public class DynamicExecEnvSnapshot {

    // immutable часть
    public final boolean volatileStats;
    public final int timeout;

    // состояние сессии (точнее потока + сессии), есть assertion что не изменяются вплоть до окончания выполнения
    public int transactTimeout; // param
    public boolean sessionVolatileStats; // ThreadLocal
    public boolean noHandled; // ThreadLocal
    public boolean inTransaction; // LockWrite
    public int secondsFromTransactStart; // LockWrite

    // преподсчитанное состояние
    public boolean isTransactTimeout = false;
    public boolean needConnectionLock;
    public boolean disableNestedLoop;
    public int setTimeout;

    public static final DynamicExecEnvSnapshot EMPTY = new DynamicExecEnvSnapshot(false, 0, Integer.MAX_VALUE);

    public DynamicExecEnvSnapshot(boolean volatileStats, int timeout, int transactTimeout) {
        this.volatileStats = volatileStats;
        this.timeout = timeout;
        this.transactTimeout = transactTimeout;
    }

    // assert что session.locked
    private void prepareEnv(SQLSession session) { // "смешивает" универсальное состояние (при отсуствии ограничений) и "местное", DynamicExecuteEnvironment.checkSnapshot выполняет обратную функцию
        noHandled = session.isNoHandled();
        if(noHandled)
            return;

        inTransaction = session.isInTransaction();
        secondsFromTransactStart = session.getSecondsFromTransactStart();

        setTimeout = timeout;
        if(setTimeout > 0) // если есть транзакция, увеличиваем timeout до времени транзакции
            setTimeout = BaseUtils.max(setTimeout, secondsFromTransactStart);

        if(session.syntax.supportsDisableNestedLoop()) {
            disableNestedLoop = volatileStats;
            sessionVolatileStats = session.isVolatileStats();
            if (sessionVolatileStats) { // проверяем локальный volatileStats
                disableNestedLoop = true;
                setTimeout = 0; // выключаем timeout
            }
        }

        // уменьшаем timeout до локального максимума
        if(inTransaction && !session.isNoTransactTimeout() && transactTimeout > 0 && (setTimeout >= transactTimeout || setTimeout == 0)) {
            setTimeout = transactTimeout;
            isTransactTimeout = true;
        }

        needConnectionLock = disableNestedLoop || (setTimeout > 0 && session.syntax.hasJDBCTimeoutMultiThreadProblem()); // проверка на timeout из-за бага в драйвере postgresql
    }

    // после readLock сессии, но до получения connection'а
    public void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException {
        prepareEnv(session);

        if(needConnectionLock)
            session.lockNeedPrivate();
    }

    public void afterConnection(SQLSession session, OperationOwner owner) throws SQLException {
        if(needConnectionLock)
            session.lockTryCommon(owner);
    }

    public boolean needConnectionLock() {
        return needConnectionLock;
    }

    public void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        if(disableNestedLoop) {
            assert needConnectionLock; // чтобы запрещать connection должен быть заблокирован
            sqlSession.setEnableNestLoop(connection, owner, false);
        }
    }

    public void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        if(disableNestedLoop) {
            assert needConnectionLock;
            sqlSession.setEnableNestLoop(connection, owner, true);
        }
    }

    public void beforeExec(Statement statement, SQLSession session) throws SQLException {
        if(setTimeout > 0)
            statement.setQueryTimeout(setTimeout);
    }

    public ImMap<SQLQuery, String> getMaterializedQueries() {
        return MapFact.EMPTY();
    }
}
