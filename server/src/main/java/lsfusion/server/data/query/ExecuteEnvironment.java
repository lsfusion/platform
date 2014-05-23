package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.AbstractTranslateValues;
import lsfusion.server.data.ExConnection;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

import java.sql.SQLException;

public class ExecuteEnvironment extends AbstractTranslateValues<ExecuteEnvironment> implements TypeEnvironment {

    public final static ExecuteEnvironment EMPTY = new ExecuteEnvironment();
    public final static ExecuteEnvironment NOREADONLY = new ExecuteEnvironment(true);

    private boolean noReadOnly;
    private boolean volatileStats;
    private boolean noPrepare;

    private ImSet<ImList<Type>> recursions;
    private ImSet<ConcatenateType> concTypes;
    private ImSet<Type> safeCastTypes;
    private ImSet<Pair<GroupType, ImList<Type>>> groupAggOrders;

    public ExecuteEnvironment() {
        this(false);
    }

    public ExecuteEnvironment(boolean noReadOnly) {
        this.noReadOnly = noReadOnly;
        this.volatileStats = false;
        this.noPrepare = false;

        recursions = SetFact.EMPTY();
        concTypes = SetFact.EMPTY();
        safeCastTypes = SetFact.EMPTY();
        
        groupAggOrders = SetFact.EMPTY();
    }

    public void add(ExecuteEnvironment environment) {
        noReadOnly = noReadOnly || environment.noReadOnly;
        volatileStats = volatileStats || environment.volatileStats;
        noPrepare = noPrepare || environment.noPrepare;

        recursions = recursions.merge(environment.recursions);
        concTypes = concTypes.merge(environment.concTypes);
        safeCastTypes = safeCastTypes.merge(environment.safeCastTypes);
        
        groupAggOrders = groupAggOrders.merge(environment.groupAggOrders);
    }

    public void addNoReadOnly() {
        noReadOnly = true;
    }

    public void addVolatileStats() {
        volatileStats = true;
    }

    public void addNoPrepare() {
        noPrepare = true;
    }

    public void addNeedRecursion(ImList<Type> types) {
        recursions = recursions.merge(types);
    }

    public void addNeedType(ConcatenateType types) {
        concTypes = concTypes.merge(types);
    }

    public void addNeedSafeCast(Type type) {
        safeCastTypes = safeCastTypes.merge(type);
    }

    public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
        groupAggOrders = groupAggOrders.merge(new Pair<GroupType, ImList<Type>>(groupType, types));
    }

    public void before(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        for(ConcatenateType concType : concTypes)
            sqlSession.typePool.ensureConcType(concType);
        for(ImList<Type> recursion : recursions)
            sqlSession.typePool.ensureRecursion(recursion);
        for(Type type : safeCastTypes)
            sqlSession.typePool.ensureSafeCast(type);
        for(Pair<GroupType, ImList<Type>> gaOrder : groupAggOrders)
            sqlSession.typePool.ensureGroupAggOrder(gaOrder);

        if(noReadOnly)
            sqlSession.pushNoReadOnly(connection.sql);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.pushVolatileStats(connection.sql, owner);
    }

    public void after(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
        if(noReadOnly)
            sqlSession.popNoReadOnly(connection.sql);
        if(volatileStats || command.length() > Settings.get().getCommandLengthVolatileStats())
            sqlSession.popVolatileStats(connection.sql, owner);
    }

    public boolean isNoPrepare() {
        return noPrepare;
    }

    public ExecuteEnvironment translateValues(MapValuesTranslate translate) {
        return this;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        throw new RuntimeException("not supported yet");
    }

    public int immutableHashCode() {
        throw new RuntimeException("not supported yet");
    }
}
