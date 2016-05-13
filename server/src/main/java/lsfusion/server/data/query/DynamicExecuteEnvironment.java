package lsfusion.server.data.query;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.*;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.form.navigator.SQLSessionUserProvider;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

// Mutable !!! нужен Thread Safe
public abstract class DynamicExecuteEnvironment<OE, S extends DynamicExecEnvSnapshot<OE, S>> {

    public abstract S getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<OE, S> outerEnv); // nullable последний параметр

    public abstract void succeeded(SQLCommand command, S snapshot, long l, DynamicExecEnvOuter<OE, S> outerEnv);

    public abstract TypeExecuteEnvironment getType();

    public abstract void failed(SQLCommand command, S snapshot);

    private static class DisableNestLoopSnapshot implements DynamicExecEnvSnapshot<Object, DisableNestLoopSnapshot> {
        @Override
        public void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface runTime) throws SQLException, SQLHandledException {
        }

        @Override
        public void afterOuter(SQLSession session, OperationOwner owner) throws SQLException {
        }

        @Override
        public void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException {
        }

        @Override
        public void afterConnection(SQLSession session, OperationOwner owner) throws SQLException {
        }

        @Override
        public void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            sqlSession.setEnableNestLoop(connection, owner, false);
        }

        @Override
        public void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            sqlSession.setEnableNestLoop(connection, owner, true);
        }

        @Override
        public void beforeExec(Statement statement, SQLSession session) throws SQLException {
        }

        @Override
        public boolean hasRepeatCommand() {
            return false;
        }

        @Override
        public boolean isTransactTimeout() {
            return false;
        }

        @Override
        public boolean needConnectionLock() {
            return true;
        }

        @Override
        public DisableNestLoopSnapshot forAnalyze() {
            return this;
        }

        @Override
        public ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries() {
            return MapFact.EMPTY();
        }

        @Override
        public Object getOuter() {
            return null;
        }

        @Override
        public DisableNestLoopSnapshot getSnapshot() {
            return this;
        }
    }

    public final static DynamicExecuteEnvironment<Object, DisableNestLoopSnapshot> DISABLENESTLOOP = new DynamicExecuteEnvironment<Object, DisableNestLoopSnapshot>() {
        public DisableNestLoopSnapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<Object, DisableNestLoopSnapshot> outerEnv) {
            return new DisableNestLoopSnapshot();
        }

        public TypeExecuteEnvironment getType() {
            return null;
        }

        public DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> createOuter(Object env) {
            return null;
        }

        @Override
        public void succeeded(SQLCommand command, DisableNestLoopSnapshot snapshot, long l, DynamicExecEnvOuter<Object, DisableNestLoopSnapshot> outerEnv) {
        }

        @Override
        public void failed(SQLCommand command, DisableNestLoopSnapshot snapshot) {
            assert false; // по идее такого не может быть
        }
    };

    public final static DynamicExecuteEnvironment<Object, AdjustVolatileExecuteEnvironment.Snapshot> DEFAULT = new DynamicExecuteEnvironment<Object, AdjustVolatileExecuteEnvironment.Snapshot>() {
        public AdjustVolatileExecuteEnvironment.Snapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> outerEnv) {
            return new AdjustVolatileExecuteEnvironment.Snapshot(false, 0, transactTimeout);
        }

        public TypeExecuteEnvironment getType() {
            return TypeExecuteEnvironment.NONE;
        }

        public DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> createOuter(Object env) {
            return null;
        }

        public void succeeded(SQLCommand command, AdjustVolatileExecuteEnvironment.Snapshot snapshot, long l, DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> outerEnv) {
        }

        public void failed(SQLCommand command, AdjustVolatileExecuteEnvironment.Snapshot snapshot) {
            assert false; // по идее такого не может быть
        }
    };

    private static Map<Integer, Integer> userExecEnvs = MapFact.getGlobalConcurrentHashMap();

    public static void setUserExecEnv(Integer user, Integer type) {
        if(type == null)
            userExecEnvs.remove(user);
        else
            userExecEnvs.put(user, type);
    }

    public static Integer getUserExecEnv(SQLSessionUserProvider userProvider) {
        Integer currentUser = userProvider.getCurrentUser();
        if(currentUser == null)
            return null;
        return userExecEnvs.get(currentUser);
    }

    public static <OE, S extends DynamicExecEnvSnapshot<OE, S>> DynamicExecEnvOuter<OE, S> create(final OE outerEnv) {
        return new DynamicExecEnvOuter<OE, S>() {
            public OE getOuter() {
                return outerEnv; // nullable
            }

            public S getSnapshot() {
                return null;
            }
        };
    }

}
