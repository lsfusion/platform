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
import lsfusion.server.data.ModifyQuery;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NotNullKeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.translator.MapValuesTranslator;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
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
    public String checkAggregation(SQLSession session) throws SQLException {
        session.pushVolatileStats(null);

        String message = "";

        ImOrderMap<ImMap<T, Object>, ImMap<String, Object>> checkResult = getRecalculateQuery(true).execute(session);
        if(checkResult.size() > 0) {
            message += "---- Checking Aggregations : " + this + "-----" + '\n';
            for(int i=0,size=checkResult.size();i<size;i++)
                message += "Keys : " + checkResult.getKey(i) + " : " + checkResult.getValue(i) + '\n';
        }

        session.popVolatileStats(null);

        return message;
    }

    public Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, false, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(ImMap<T, ? extends Expr> joinImplement) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, true, PropertyChanges.EMPTY, null);
    }

    private Query<T, String> getRecalculateQuery(boolean outDB) {
        QueryBuilder<T, String> query = new QueryBuilder<T, String>(this);

        Expr dbExpr = getExpr(query.getMapExprs());
        Expr calculateExpr = calculateExpr(query.getMapExprs());
        if(outDB)
            query.addProperty("dbvalue", dbExpr);
        query.addProperty("calcvalue", calculateExpr);
        query.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        query.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not());
        return query.getQuery();
    }

    public static AggregateProperty recalculate = null;

    @Message("logics.info.recalculation.of.aggregated.property")
    @ThisMessage
    public void recalculateAggregation(SQLSession session) throws SQLException {
        session.pushVolatileStats(null);
        session.modifyRecords(new ModifyQuery(mapTable.table, getRecalculateQuery(false).map(
                mapTable.mapKeys.reverse(), MapFact.singletonRev(field, "calcvalue"), MapValuesTranslator.noTranslate)));
        session.popVolatileStats(null);
    }

    @IdentityLazy
    public ClassWhere<Object> getClassValueWhere(ClassType type) {
        if(type == ClassType.ASSERTFULL) {
            assert isFull();
            return getClassValueWhere(ClassType.ASIS);
        }
        if(type == ClassType.FULL) {
            ClassWhere<Object> result = getClassValueWhere(ClassType.ASIS);
            if(!isFull())
                result = result.and(new ClassWhere<Object>(BaseUtils.<ImMap<Object, ValueClass>>immutableCast(getInterfaceCommonClasses(null)), true));
            return result;
        }

        ImRevMap<T,NotNullKeyExpr> mapExprs = interfaces.mapRevValues(new GetIndexValue<NotNullKeyExpr, T>() {
            public NotNullKeyExpr getMapValue(int i, T value) {
                return new NotNullKeyExpr(i);
            }});
        return Query.getClassWhere(Where.TRUE, mapExprs, MapFact.singleton((Object)"value", calculateClassExpr(mapExprs)));
    }
}
