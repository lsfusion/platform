package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataSession;
import platform.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.singletonMap;

public abstract class AggregateProperty<T extends PropertyInterface> extends CalcProperty<T> {

    public boolean isStored() {
        assert (field!=null) == (mapTable!=null);
        return mapTable!=null && !DataSession.reCalculateAggr; // для тестирования 2-е условие
    }

    protected AggregateProperty(String SID,String caption,List<T> interfaces) {
        super(SID,caption,interfaces);
    }

    // проверяет аггрегацию для отладки
    @ThisMessage
    @Message("logics.info.checking.aggregated.property")
    public String checkAggregation(SQLSession session) throws SQLException {
        session.pushVolatileStats(null);

        String message = "";

        OrderedMap<Map<T, Object>, Map<String, Object>> checkResult = getRecalculateQuery(true).execute(session);
        if(checkResult.size() > 0) {
            message += "---- Checking Aggregations : " + this + "-----" + '\n';
            for(Map.Entry<Map<T,Object>,Map<String,Object>> row : checkResult.entrySet())
                message += "Keys : " + row.getKey() + " : " + row.getValue() + '\n';
        }

        session.popVolatileStats(null);

        return message;
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, false, PropertyChanges.EMPTY, null);
    }

    public Expr calculateClassExpr(Map<T, ? extends Expr> joinImplement) { // вызывается до stored, поэтому чтобы не было проблем с кэшами, сделано так
        return calculateExpr(joinImplement, true, PropertyChanges.EMPTY, null);
    }

    private Query<T, String> getRecalculateQuery(boolean outDB) {
        Query<T, String> query = new Query<T, String>(this);

        Expr dbExpr = getExpr(query.mapKeys);
        Expr calculateExpr = calculateExpr(query.mapKeys);
        if(outDB)
            query.properties.put("dbvalue", dbExpr);
        query.properties.put("calcvalue", calculateExpr);
        query.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        query.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not());
        return query;
    }

    public static AggregateProperty recalculate = null;

    @Message("logics.info.recalculation.of.aggregated.property")
    @ThisMessage
    public void recalculateAggregation(SQLSession session) throws SQLException {
        session.pushVolatileStats(null);
        session.modifyRecords(new ModifyQuery(mapTable.table, getRecalculateQuery(false).map(
                BaseUtils.reverse(mapTable.mapKeys), singletonMap(field, "calcvalue"), MapValuesTranslator.noTranslate)));
        session.popVolatileStats(null);
    }

    @IdentityLazy
    public ClassWhere<Object> getClassValueWhere() {
        Query<T, String> query = new Query<T, String>(this);
        query.properties.put("value", calculateClassExpr(query.mapKeys));
        return query.getClassWhere(Collections.singleton("value"));
    }
}
