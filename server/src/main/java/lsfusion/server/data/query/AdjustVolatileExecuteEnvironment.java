package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.Settings;
import lsfusion.server.data.*;
import lsfusion.server.data.type.ParseInterface;

import java.sql.SQLException;
import java.sql.Statement;

public class AdjustVolatileExecuteEnvironment extends DynamicExecuteEnvironment<Object, AdjustVolatileExecuteEnvironment.Snapshot> {

    // если volatileStats, то assertion что запрос без volatileStats с заданным timeout'ом не выполнился
    private boolean volatileStats;
    private int timeout = Settings.get().getTimeoutStart();

    public synchronized Snapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<Object, Snapshot> outerEnv) {
        return new Snapshot(volatileStats, timeout, transactTimeout);
    }

    public TypeExecuteEnvironment getType() {
        return TypeExecuteEnvironment.DISABLENESTLOOP;
    }

    // метод "обратный" prepareEnv'у, его задача "размешать" локальное и глобальное состояние, то есть определить когда локальное состояние мешает глобальному
    private boolean checkSnapshot(Snapshot snapshot) {
        if(snapshot.noHandled)
            return false;

        if(!(volatileStats == snapshot.volatileStats && timeout == snapshot.timeout)) // discard'м если состояние на конец отличается от состояния на начало
            return false;

        if(timeout == 0 || snapshot.isTransactTimeout) // уже выключен, snapshot хочет volatile, а сессия нет, включился transactTimeout
            return false;

        return true;
    }

    public synchronized void succeeded(SQLCommand command, Snapshot snapshot, long l, DynamicExecEnvOuter<Object, Snapshot> outerEnv) {
        if(snapshot.volatileStats && timeout > snapshot.secondsFromTransactStart) { // проверка checkSnapshot не первая для оптимизации
            if(!checkSnapshot(snapshot))
                return;

            assert volatileStats;
            // только если больше чем secondsFromTransactStart, потому как в противном случае без volatileStats с большой вероятностью выполнялось для меньшего timeout'а
            timeout = 0; // то есть без volatileStats не выполнилось, а с volatileStats выполнилось - помечаем запрос как опасный, точнее выключаем env
        }
    }

    public synchronized void failed(SQLCommand command, Snapshot snapshot) {
        if(!checkSnapshot(snapshot))
            return;

        int degree = Settings.get().getTimeoutDegree();
        if(timeout < snapshot.setTimeout) {
            assert snapshot.setTimeout == snapshot.secondsFromTransactStart; // так как увеличить timeout может только транзакция
            timeout = snapshot.setTimeout;
        } else {
            assert !snapshot.isTransactTimeout;
            if(volatileStats)
                timeout *= degree;
        }
        volatileStats = !volatileStats;
    }

    public DynamicExecEnvOuter<Object, Snapshot> createOuter(Object env) {
        return null;
    }

    public static class Snapshot implements DynamicExecEnvSnapshot<Object, Snapshot> {
        // immutable часть
        public final boolean volatileStats;
        public final int timeout;

        // состояние сессии (точнее потока + сессии), есть assertion что не изменяются вплоть до окончания выполнения
        public final int transactTimeout; // param

        public boolean noHandled; // ThreadLocal
        public boolean inTransaction; // LockWrite
        public int secondsFromTransactStart; // LockWrite

        // преподсчитанное состояние
        public boolean isTransactTimeout = false;
        public boolean needConnectionLock;
        public boolean disableNestedLoop;
        public int setTimeout;

        public Snapshot(boolean volatileStats, int timeout, int transactTimeout) {
            this.volatileStats = volatileStats;
            this.timeout = timeout;
            this.transactTimeout = transactTimeout;
        }

        private boolean forAnalyze;

        public Snapshot(boolean volatileStats, int timeout, int transactTimeout, boolean forAnalyze) {
            assert forAnalyze;
            this.volatileStats = volatileStats;
            this.timeout = timeout;
            this.transactTimeout = transactTimeout;
            this.forAnalyze = forAnalyze;
        }

        public Snapshot forAnalyze() {
            assert !forAnalyze;

            return new Snapshot(volatileStats, timeout, transactTimeout, true);
        }

        public void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface runTime) throws SQLException {
        }

        public Object getOuter() {
            return null;
        }

        public Snapshot getSnapshot() {
            throw new UnsupportedOperationException();
        }

        public void afterOuter(SQLSession session, OperationOwner owner) throws SQLException {
        }

        // assert что session.locked
        private void prepareEnv(SQLSession session) { // "смешивает" универсальное состояние (при отсуствии ограничений) и "местное", DynamicExecuteEnvironment.checkSnapshot выполняет обратную функцию
            noHandled = session.isNoHandled();
            if(noHandled)
                return;

            inTransaction = session.isInTransaction();
            setTimeout = timeout;

            if(session.isInTransaction() && session.syntax.hasTransactionSavepointProblem() && !Settings.get().isUseSavepointsForExceptions()) { // если нет savepoint'ов увеличиваем до времени с начала транзакции
                secondsFromTransactStart = session.getSecondsFromTransactStart();
                if (setTimeout > 0) // если есть транзакция, увеличиваем timeout до времени транзакции
                    setTimeout = BaseUtils.max(setTimeout, secondsFromTransactStart);
            }

            if(session.syntax.supportsDisableNestedLoop()) {
                disableNestedLoop = volatileStats;
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

        public boolean isTransactTimeout() {
            return isTransactTimeout;
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

        public boolean hasRepeatCommand() {
            return setTimeout > 0;
        }

        public ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries() {
            return MapFact.EMPTY();
        }

    }
}
