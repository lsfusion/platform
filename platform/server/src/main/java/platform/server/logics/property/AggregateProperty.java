package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.*;
import platform.server.classes.ValueClass;
import platform.server.classes.CustomClass;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.session.DataSession;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;

import java.sql.SQLException;
import java.util.*;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

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
        Query<T, String> aggrSelect;
        aggrSelect = getQuery("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        OrderedMap<Map<T, Object>, Map<String, Object>> aggrResult = aggrSelect.execute(session);
        DataSession.reCalculateAggr = true;
        aggrSelect = getQuery("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("RECALCULATE - "+caption);
            AggrSelect.outSelect(Session);
        }*/
//        if(BusinessLogics.ChangeDBIteration==13 && caption.equals("Посл. дата изм. парам.")) {
//            System.out.println("RECALCULATE - "+caption);
//            AggrSelect.outSelect(Session);
//        }

        OrderedMap<Map<T, Object>, Map<String, Object>> calcResult = aggrSelect.execute(session);
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
        PropertyField writeField = field;
        field = null;
        recalculate = this;

        Query<KeyField, PropertyField> writeQuery = new Query<KeyField, PropertyField>(mapTable.table);
        Expr recalculateExpr = getExpr(BaseUtils.join(mapTable.mapKeys, writeQuery.mapKeys));
        writeQuery.properties.put(writeField, recalculateExpr);
        writeQuery.and(mapTable.table.joinAnd(writeQuery.mapKeys).getWhere().or(recalculateExpr.getWhere()));

        session.modifyRecords(new ModifyQuery(mapTable.table,writeQuery));

        field = writeField;
    }

    int getCoeff(PropertyMapImplement<?, T> implement) { return 0; }

    public Type getType() {
        return calculateExpr(getMapKeys()).getSelfType();
    }

    protected Map<T, ValueClass> getMapClasses() {
        Query<T, String> query = new Query<T, String>(this);
        query.and(calculateExpr(query.mapKeys).getWhere());
        return query.<T>getClassWhere(new ArrayList<String>()).getCommonParent(interfaces);
    }

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Query<KeyField, Field> query = new Query<KeyField,Field>(mapTable.table);
        Expr expr = calculateExpr(BaseUtils.join(mapTable.mapKeys, query.mapKeys));
        query.properties.put(storedField,expr);
        query.and(expr.getWhere());
        return query.getClassWhere(Collections.singleton(storedField));
    }

    protected boolean usePreviousStored() {
        return true;
    }
}
