package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
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
    public boolean checkAggregation(SQLSession session,String caption) throws SQLException {
        Map<T, KeyExpr> mapKeys = getMapKeys();

        OrderedMap<Map<T, Object>, Map<String, Object>> aggrResult = new Query<T,String>(mapKeys,getExpr(mapKeys),"value").execute(session);
        DataSession.reCalculateAggr = true;
        OrderedMap<Map<T, Object>, Map<String, Object>> calcResult = new Query<T,String>(mapKeys,getExpr(mapKeys),"value").execute(session);
        DataSession.reCalculateAggr = false;

        Iterator<Map.Entry<Map<T,Object>,Map<String,Object>>> i = aggrResult.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Map<T,Object>,Map<String,Object>> row = i.next();
            Map<T, Object> rowKey = row.getKey();
            Object rowValue = dropZero(row.getValue().get("value"));
            Map<String,Object> calcRow = calcResult.get(rowKey);
            Object calcValue = (calcRow==null?null:dropZero(calcRow.get("value")));
            if(rowValue==calcValue || (rowValue!=null && rowValue.equals(calcValue))) {
                i.remove();
                calcResult.remove(rowKey);
            }
        }
        // вычистим и отсюда 0
        i = calcResult.entrySet().iterator();
        while(i.hasNext()) {
            if(dropZero(i.next().getValue().get("value"))==null)
                i.remove();
        }

        if(calcResult.size()>0 || aggrResult.size()>0) {
            System.out.println("----CheckAggregations "+caption+"-----");
            System.out.println("----Aggr-----");
            for(Map.Entry<Map<T,Object>,Map<String,Object>> row : aggrResult.entrySet())
                System.out.println(row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<T,Object>,Map<String,Object>> row : calcResult.entrySet())
                System.out.println(row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    public static AggregateProperty recalculate = null;
    
    public void recalculateAggregation(SQLSession session) throws SQLException {
        stored = false;
        recalculate = this;

        Query<KeyField, PropertyField> writeQuery = new Query<KeyField, PropertyField>(mapTable.table);
        Expr recalculateExpr = getExpr(BaseUtils.join(mapTable.mapKeys, writeQuery.mapKeys));
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

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Query<KeyField, Field> query = new Query<KeyField,Field>(mapTable.table);
        Expr expr = calculateClassExpr(BaseUtils.join(mapTable.mapKeys, query.mapKeys));
        query.properties.put(storedField,expr);
        query.and(expr.getWhere());
        return query.getClassWhere(Collections.singleton(storedField));
    }

    protected boolean usePreviousStored() {
        return true;
    }
}
