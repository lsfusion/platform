package lsfusion.server.data.query.exec;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.exec.materialize.NotMaterializable;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypePool;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.Settings;

import java.sql.Connection;
import java.sql.SQLException;

public class StaticExecuteEnvironmentImpl extends TwinImmutableObject implements StaticExecuteEnvironment, MStaticExecuteEnvironment {

    public final static MStaticExecuteEnvironment MVOID = mEnv(); // вникуда только для toString
    public static MStaticExecuteEnvironment mEnv() {
        return new StaticExecuteEnvironmentImpl(false);
    }

    public StaticExecuteEnvironmentImpl(StaticExecuteEnvironmentImpl env) {
        this.noReadOnly = env.noReadOnly;
        this.volatileStats = env.volatileStats;
        this.noPrepare = env.noPrepare;
        this.recursions = env.recursions;
        this.concTypes = env.concTypes;
        this.safeCastTypes = env.safeCastTypes;
        this.groupAggOrders = env.groupAggOrders;
        this.typeFuncs = env.typeFuncs;
        this.arrayClasses = env.arrayClasses;
        this.usedNotMaterializables = env.usedNotMaterializables;
        finalized = false;
    }

    public static MStaticExecuteEnvironment mEnv(StaticExecuteEnvironment env) {
        return new StaticExecuteEnvironmentImpl((StaticExecuteEnvironmentImpl) env);
    }

    // IMMUTABLE
    public final static StaticExecuteEnvironment EMPTY = new StaticExecuteEnvironmentImpl(true);
    public final static StaticExecuteEnvironment NOREADONLY = new StaticExecuteEnvironmentImpl(true, true);

    private StaticExecuteEnvironmentImpl(boolean finalized) {
        this(finalized, false);
    }

    private StaticExecuteEnvironmentImpl(boolean finalized, boolean noReadOnly) {
        this.noReadOnly = noReadOnly;
        this.volatileStats = false;
        this.noPrepare = false;

        recursions = SetFact.EMPTYORDER();
        concTypes = SetFact.EMPTY();
        safeCastTypes = SetFact.EMPTY();

        groupAggOrders = SetFact.EMPTY();
        arrayClasses = SetFact.EMPTY();
        typeFuncs = SetFact.EMPTY();

        usedNotMaterializables = SetFact.EMPTY();

        this.finalized = finalized;
    }

    private boolean noReadOnly;
    private boolean volatileStats;
    private boolean noPrepare;

    private ImOrderSet<Object> recursions;
    private ImSet<ConcatenateType> concTypes;
    private ImSet<Pair<Type, Boolean>> safeCastTypes;
    private ImSet<Pair<GroupType, ImList<Type>>> groupAggOrders;

    private ImSet<NotMaterializable> usedNotMaterializables;

    private ImSet<Pair<TypeFunc, Type>> typeFuncs;

    private ImSet<ArrayClass> arrayClasses;

    private boolean finalized;

    public void add(StaticExecuteEnvironment environment) {
        assert !finalized;

        StaticExecuteEnvironmentImpl envImpl = (StaticExecuteEnvironmentImpl)environment;
        noReadOnly = noReadOnly || envImpl.noReadOnly;
        volatileStats = volatileStats || envImpl.volatileStats;
        noPrepare = noPrepare || envImpl.noPrepare;

        recursions = recursions.mergeOrder(envImpl.recursions);
        concTypes = concTypes.merge(envImpl.concTypes);
        safeCastTypes = safeCastTypes.merge(envImpl.safeCastTypes);

        groupAggOrders = groupAggOrders.merge(envImpl.groupAggOrders);
        arrayClasses = arrayClasses.merge(envImpl.arrayClasses);
        typeFuncs = typeFuncs.merge(envImpl.typeFuncs);

        usedNotMaterializables = usedNotMaterializables.merge(envImpl.usedNotMaterializables);
    }

    public void addNotMaterializable(NotMaterializable table) {
        assert !finalized;

        usedNotMaterializables = usedNotMaterializables.merge(table);
    }

    public void removeNotMaterializable(NotMaterializable table) {
        assert !finalized;

        usedNotMaterializables = usedNotMaterializables.removeIncl(table); // по идее не может исчезнуть STEP вообще
    }

    public void addNoReadOnly() {
        assert !finalized;

        noReadOnly = true;
    }

    public void addVolatileStats() {
        assert !finalized;

        volatileStats = true;
    }

    public void addNoPrepare() {
        assert !finalized;

        noPrepare = true;
    }

    public void addNeedRecursion(Object types) {
        assert !finalized;

        recursions = recursions.mergeOrder(types);
    }

    public void addNeedType(ConcatenateType types) {
        assert !finalized;

        concTypes = concTypes.merge(types);
    }

    public void addNeedArrayClass(ArrayClass arrayClass) {
        assert !finalized;

        arrayClasses = arrayClasses.merge(arrayClass);
    }

    public void addNeedTableType(SessionTable.TypeStruct tableType) {
        assert !finalized;

        throw new UnsupportedOperationException();
    }

    public void addNeedSafeCast(Type type, Boolean isInt) {
        assert !finalized;

        safeCastTypes = safeCastTypes.merge(new Pair<>(type, isInt));
    }

    public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
        assert !finalized;

        groupAggOrders = groupAggOrders.merge(new Pair<>(groupType, types));
    }

    public void addNeedTypeFunc(TypeFunc typeFunc, Type type) {
        assert !finalized;

        typeFuncs = typeFuncs.merge(new Pair<>(typeFunc, type));
    }

    public StaticExecuteEnvironment finish() {
        finalized = true;

        return this;
    }

    public void before(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        before(connection.sql, sqlSession.typePool, sqlSession, command, owner);
    }
    public void after(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        after(connection.sql, sqlSession.typePool, sqlSession, command, owner, null);
    }

    public Object before(Connection connection, TypePool typePool, String command, OperationOwner owner) throws SQLException {
        return before(connection, typePool, null, command, owner);
    }

    public void after(Connection connection, TypePool typePool, String command, OperationOwner owner, Object prevEnvState) throws SQLException {
        after(connection, typePool, null, command, owner, prevEnvState);
    }

    public Object before(Connection connection, TypePool typePool, SQLSession sqlSession, String command, OperationOwner owner) throws SQLException {
        assert finalized;
        
        Object prevEnvState = null;

        for(ConcatenateType concType : concTypes)
            typePool.ensureConcType(concType);
        for(ArrayClass arrayClass : arrayClasses)
            typePool.ensureArrayClass(arrayClass);
        for(Object recursion : recursions)
            typePool.ensureRecursion(recursion);
        for(Pair<Type, Boolean> type : safeCastTypes)
            typePool.ensureSafeCast(type);
        for(Pair<GroupType, ImList<Type>> gaOrder : groupAggOrders)
            typePool.ensureGroupAggOrder(gaOrder);
        for(Pair<TypeFunc, Type> tf : typeFuncs)
            typePool.ensureTypeFunc(tf);

        if(noReadOnly) {
            if(sqlSession == null) {
                prevEnvState = connection.isReadOnly();
                connection.setReadOnly(false);
            } else
                sqlSession.pushNoReadOnly(connection);
        }
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.pushVolatileStats(owner);
        
        return prevEnvState;
    }

    public void after(Connection connection, TypePool typePool, SQLSession sqlSession, String command, OperationOwner owner, Object prevEnvState) throws SQLException {
        assert finalized;

        if(noReadOnly) {
            if(sqlSession == null)
                connection.setReadOnly((boolean) prevEnvState);
            else
                sqlSession.popNoReadOnly(connection);
        }
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.popVolatileStats(owner);
    }

    @Override
    public boolean hasNotMaterializable() {
        assert finalized;

        return !usedNotMaterializables.isEmpty();
    }

    public boolean hasRecursion() {
        assert finalized;

        return !recursions.isEmpty();
    }

    public boolean isNoPrepare() {
        assert finalized;

        return noPrepare;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        throw new RuntimeException("not supported yet");
    }

    public int immutableHashCode() {
        throw new RuntimeException("not supported yet");
    }

    private final EnsureTypeEnvironment ensureTypes = new EnsureTypeEnvironment() {
        public void addNeedRecursion(Object types) {
            assert recursions.contains(types);
        }

        public void addNeedType(ConcatenateType concType) {
            assert concTypes.contains(concType);
        }

        public void addNeedTableType(SessionTable.TypeStruct tableType) {
            throw new UnsupportedOperationException();
        }

        public void addNeedArrayClass(ArrayClass tableType) {
            assert arrayClasses.contains(tableType);
        }

        public void addNeedSafeCast(Type type, Boolean isInt) {
            assert safeCastTypes.contains(new Pair<>(type, isInt));
        }

        public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
            assert groupAggOrders.contains(new Pair<>(groupType, types));
        }

        public void addNeedTypeFunc(TypeFunc groupType, Type type) {
            assert typeFuncs.contains(new Pair<>(groupType, type));
        }
    };
    public EnsureTypeEnvironment getEnsureTypes() {
        return ensureTypes;
    }

}
