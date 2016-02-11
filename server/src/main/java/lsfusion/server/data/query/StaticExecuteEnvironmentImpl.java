package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.AbstractTranslateValues;
import lsfusion.server.data.ExConnection;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ArrayClass;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

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
        finalized = false;
    }

    public static MStaticExecuteEnvironment mEnv(StaticExecuteEnvironment env) {
        return new StaticExecuteEnvironmentImpl((StaticExecuteEnvironmentImpl) env);
    }

    // IMMUTABLE
    public final static StaticExecuteEnvironment EMPTY = new StaticExecuteEnvironmentImpl(true);
    public final static StaticExecuteEnvironment NOREADONLY = new StaticExecuteEnvironmentImpl(true, true);

    public StaticExecuteEnvironmentImpl(boolean finalized) {
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

        this.finalized = finalized;
    }

    private boolean noReadOnly;
    private boolean volatileStats;
    private boolean noPrepare;

    private ImOrderSet<Object> recursions;
    private ImSet<ConcatenateType> concTypes;
    private ImSet<Type> safeCastTypes;
    private ImSet<Pair<GroupType, ImList<Type>>> groupAggOrders;

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

    public void addNeedSafeCast(Type type) {
        assert !finalized;

        safeCastTypes = safeCastTypes.merge(type);
    }

    public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
        assert !finalized;

        groupAggOrders = groupAggOrders.merge(new Pair<GroupType, ImList<Type>>(groupType, types));
    }

    public void addNeedTypeFunc(TypeFunc typeFunc, Type type) {
        assert !finalized;

        typeFuncs = typeFuncs.merge(new Pair<TypeFunc, Type>(typeFunc, type));
    }

    public StaticExecuteEnvironment finish() {
        finalized = true;

        return this;
    }

    public void before(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        assert finalized;

        for(ConcatenateType concType : concTypes)
            sqlSession.typePool.ensureConcType(concType);
        for(ArrayClass arrayClass : arrayClasses)
            sqlSession.typePool.ensureArrayClass(arrayClass);
        for(Object recursion : recursions)
            sqlSession.typePool.ensureRecursion(recursion);
        for(Type type : safeCastTypes)
            sqlSession.typePool.ensureSafeCast(type);
        for(Pair<GroupType, ImList<Type>> gaOrder : groupAggOrders)
            sqlSession.typePool.ensureGroupAggOrder(gaOrder);
        for(Pair<TypeFunc, Type> tf : typeFuncs)
            sqlSession.typePool.ensureTypeFunc(tf);

        if(noReadOnly)
            sqlSession.pushNoReadOnly(connection.sql);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.pushVolatileStats(owner);
    }

    public void after(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        assert finalized;

        if(noReadOnly)
            sqlSession.popNoReadOnly(connection.sql);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.popVolatileStats(owner);
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

        public void addNeedSafeCast(Type type) {
            assert safeCastTypes.contains(type);
        }

        public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
            assert groupAggOrders.contains(new Pair<GroupType, ImList<Type>>(groupType, types));
        }

        public void addNeedTypeFunc(TypeFunc groupType, Type type) {
            assert typeFuncs.contains(new Pair<TypeFunc, Type>(groupType, type));
        }
    };
    public EnsureTypeEnvironment getEnsureTypes() {
        return ensureTypes;
    }

}
