package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.singletonMap;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    public boolean stored = false;

    public boolean isStored() {
        return stored && !DataSession.reCalculateAggr; // для тестирования 2-е условие
    }

    protected AggregateProperty(String SID,String caption,List<T> interfaces) {
        super(SID,caption,interfaces);
    }

    // проверяет аггрегацию для отладки
    @ThisMessage
    @Message("logics.info.checking.aggregated.property")
    public String checkAggregation(SQLSession session) throws SQLException {
        String message = "";

        OrderedMap<Map<T, Object>, Map<String, Object>> checkResult = getRecalculateQuery(true).execute(session);
        if(checkResult.size() > 0) {
            message += "---- Checking Aggregations : " + this + "-----" + '\n';
            for(Map.Entry<Map<T,Object>,Map<String,Object>> row : checkResult.entrySet())
                message += "Keys : " + row.getKey() + " : " + row.getValue() + '\n';
        }

        return message;
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
        session.modifyRecords(new ModifyQuery(mapTable.table, getRecalculateQuery(false).map(
                BaseUtils.reverse(mapTable.mapKeys), singletonMap(field, "calcvalue"), MapValuesTranslator.noTranslate)));
    }

    int getCoeff(PropertyMapImplement<?, T> implement) { return 0; }

    @IdentityLazy
    public Type getType() {
        return calculateClassExpr(getMapKeys()).getSelfType();
    }

    // потом можно убрать когда getCommonClass будет в OrConcatenateClass
    @Override
    @IdentityLazy
    public Map<T,ValueClass> getMapClasses() {
        Query<T, String> query = new Query<T, String>(this);
        query.and(calculateClassExpr(query.mapKeys).getWhere());
        return query.<T>getClassWhere(new ArrayList<String>()).getCommonParent(interfaces);
    }

    @IdentityLazy
    public CommonClasses<T> getCommonClasses() {
        Query<T, String> query = new Query<T, String>(this);
        query.properties.put("value", calculateClassExpr(query.mapKeys));
        Map<Object, ValueClass> mapClasses = query.<Object>getClassWhere(query.properties.keySet()).getCommonParent(BaseUtils.<Object,T,String>merge(interfaces, query.properties.keySet()));
        return new CommonClasses<T>(BaseUtils.filterKeys(mapClasses, interfaces), mapClasses.get("value"));
    }

    @IdentityLazy
    public ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Query<KeyField, Field> query = new Query<KeyField,Field>(mapTable.table);
        Expr expr = calculateClassExpr(BaseUtils.join(mapTable.mapKeys, query.mapKeys));
        query.properties.put(storedField,expr);
        return query.getClassWhere(Collections.singleton(storedField));
    }
}
