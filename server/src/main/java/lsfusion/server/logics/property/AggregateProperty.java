package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.interop.Compare;
import lsfusion.server.Message;
import lsfusion.server.ThisMessage;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.NotNullKeyExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DBManager;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChanges;

import java.sql.SQLException;

public abstract class AggregateProperty<T extends PropertyInterface> extends CalcProperty<T> {

    public boolean isStored() {
        assert (field!=null) == (mapTable!=null);
        return mapTable!=null && !DataSession.reCalculateAggr; // для тестирования 2-е условие
    }

    protected AggregateProperty(String SID,String caption,ImOrderSet<T> interfaces) {
        super(SID,caption,interfaces);
    }

    // проверяет аггрегацию для отладки
    @ThisMessage
    @Message("logics.info.checking.aggregated.property")
    public String checkAggregation(SQLSession session) throws SQLException, SQLHandledException {
        session.pushVolatileStats(null, OperationOwner.unknown);
        
        try {
    
            String message = "";
    
            ImOrderMap<ImMap<T, Object>, ImMap<String, Object>> checkResult = getRecalculateQuery(true).execute(session, OperationOwner.unknown);
            if(checkResult.size() > 0) {
                message += "---- Checking Aggregations : " + this + "-----" + '\n';
                for(int i=0,size=checkResult.size();i<size;i++)
                    message += "Keys : " + checkResult.getKey(i) + " : " + checkResult.getValue(i) + '\n';
            }

            return message;
        } finally {
            session.popVolatileStats(null, OperationOwner.unknown);
        }
    }

    public Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, CalcType.EXPR, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(ImMap<T, ? extends Expr> joinImplement, PrevClasses prevSameClasses) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, prevSameClasses.getCalc(), PropertyChanges.EMPTY, null);
    }

    public Expr calculateStatExpr(ImMap<T, ? extends Expr> joinImplement) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, CalcType.STAT, PropertyChanges.EMPTY, null);
    }

    private Query<T, String> getRecalculateQuery(boolean outDB) {
        QueryBuilder<T, String> query = new QueryBuilder<T, String>(this);

        Expr dbExpr = getExpr(query.getMapExprs());
        Expr calculateExpr = calculateExpr(query.getMapExprs());
        if(outDB)
            query.addProperty("dbvalue", dbExpr);
        query.addProperty("calcvalue", calculateExpr);
        query.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        if(!DBManager.RECALC_REUPDATE)
            query.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not().and(dbExpr.getWhere().or(calculateExpr.getWhere())));
        return query.getQuery();
    }

    public static AggregateProperty recalculate = null;

    @Message("logics.info.recalculation.of.aggregated.property")
    @ThisMessage
    public void recalculateAggregation(SQLSession session) throws SQLException, SQLHandledException {
        session.pushVolatileStats(null, OperationOwner.unknown);
        try {
            session.modifyRecords(new ModifyQuery(mapTable.table, getRecalculateQuery(false).map(
                    mapTable.mapKeys.reverse(), MapFact.singletonRev(field, "calcvalue")), OperationOwner.unknown, TableOwner.global));
        } finally {
            session.popVolatileStats(null, OperationOwner.unknown);
        }
    }

    @IdentityLazy
    public ClassWhere<Object> getClassValueWhere(ClassType type, PrevClasses prevSameClasses) {
        if(type == ClassType.ASSERTFULL) {
            assert isFull();
            return getClassValueWhere(ClassType.ASIS, prevSameClasses);
        }
        if(type == ClassType.FULL) {
            ClassWhere<Object> result = getClassValueWhere(ClassType.ASIS, prevSameClasses);
            if(!isFull())
                result = result.and(new ClassWhere<Object>(BaseUtils.<ImMap<Object, ValueClass>>immutableCast(getInterfaceCommonClasses(null, prevSameClasses)), true));
            return result;
        }

        ImRevMap<T,NotNullKeyExpr> mapExprs = getMapNotNullKeys();
        return Query.getClassWhere(Where.TRUE, mapExprs, MapFact.singleton((Object)"value", calculateClassExpr(mapExprs, prevSameClasses)));
    }

    private ImRevMap<T, NotNullKeyExpr> getMapNotNullKeys() {
        return interfaces.mapRevValues(new GetIndexValue<NotNullKeyExpr, T>() {
            public NotNullKeyExpr getMapValue(int i, T value) {
                return new NotNullKeyExpr(i);
            }});
    }

    @IdentityLazy
    public StatKeys<T> getInterfaceClassStats() {
        ImRevMap<T,KeyExpr> mapKeys = getMapKeys();
        return calculateStatExpr(mapKeys).getWhere().getStatKeys(mapKeys.valuesSet()).mapBack(mapKeys);
    }

    public boolean hasAlotKeys() {
        return Stat.ALOT.lessEquals(getInterfaceClassStats().rows);
    }
}
