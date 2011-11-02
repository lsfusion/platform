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
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    public boolean stored = false;

    public boolean isStored() {
        return stored && !DataSession.reCalculateAggr; // для тестирования 2-е условие
    }

    protected AggregateProperty(String SID,String caption,List<T> interfaces) {
        super(SID,caption,interfaces);
    }

    Object dropZero(Object Value) {
        if(Value instanceof Integer && Value.equals(0)) return null;
        if(Value instanceof Long && ((Long)Value).intValue()==0) return null;
        if(Value instanceof Double && ((Double)Value).intValue()==0) return null;
        if(Value instanceof Boolean && !((Boolean)Value)) return null;
        return Value;
    }

    // проверяет аггрегацию для отладки
    @ThisMessage
    @Message("logics.info.checking.aggregated.property")
    public String checkAggregation(SQLSession session) throws SQLException {
        String message = "";

        Query<T, String> checkQuery = new Query<T, String>(this);

        Expr dbExpr = getExpr(checkQuery.mapKeys);
        Expr calculateExpr = calculateExpr(checkQuery.mapKeys);
        checkQuery.properties.put("dbvalue", dbExpr);
        checkQuery.properties.put("calcvalue", calculateExpr);
        checkQuery.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        checkQuery.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not());

        OrderedMap<Map<T, Object>, Map<String, Object>> checkResult = checkQuery.execute(session);
        if(checkResult.size() > 0) {
            message += "---- Checking Aggregations : " + this + "-----" + '\n';
            for(Map.Entry<Map<T,Object>,Map<String,Object>> row : checkResult.entrySet())
                message += "Keys : " + row.getKey() + " : " + row.getValue() + '\n';
        }

        return message;
    }

    public static AggregateProperty recalculate = null;

    @Message("logics.info.recalculation.of.aggregated.property")
    @ThisMessage
    public void recalculateAggregation(SQLSession session) throws SQLException {
        Query<KeyField, PropertyField> writeQuery = new Query<KeyField, PropertyField>(mapTable.table);
        Expr recalculateExpr = calculateExpr(BaseUtils.join(mapTable.mapKeys, writeQuery.mapKeys));
        writeQuery.properties.put(field, recalculateExpr);
        writeQuery.and(mapTable.table.joinAnd(writeQuery.mapKeys).getWhere().or(recalculateExpr.getWhere()));

        session.modifyRecords(new ModifyQuery(mapTable.table,writeQuery));
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
        query.and(expr.getWhere());
        return query.getClassWhere(Collections.singleton(storedField));
    }
}
