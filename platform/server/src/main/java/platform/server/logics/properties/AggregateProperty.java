package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.session.DataSession;
import platform.server.session.SQLSession;

import java.sql.SQLException;
import java.util.*;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    protected AggregateProperty(String iSID,Collection<T> iInterfaces) {
        super(iSID,iInterfaces);
    }

    public ValueClass getValueClass() {
        return getQuery("value").getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();
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
        JoinQuery<T, String> aggrSelect;
        aggrSelect = getQuery("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<T, Object>, Map<String, Object>> aggrResult = aggrSelect.executeSelect(session);
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

        LinkedHashMap<Map<T, Object>, Map<String, Object>> calcResult = aggrSelect.executeSelect(session);
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
            for(Map.Entry<Map<T,Object>,Map<String,Object>> Row : calcResult.entrySet())
                System.out.println(Row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    public void recalculateAggregation(SQLSession session) throws SQLException {
        PropertyField writeField = field;
        field = null;

        JoinQuery<KeyField, PropertyField> writeQuery = new JoinQuery<KeyField, PropertyField>(mapTable.table);
        SourceExpr recalculateExpr = getSourceExpr(BaseUtils.join(mapTable.mapKeys, writeQuery.mapKeys));
        writeQuery.properties.put(writeField, recalculateExpr);
        writeQuery.and(mapTable.table.joinAnd(writeQuery.mapKeys).getWhere().or(recalculateExpr.getWhere()));

        session.modifyRecords(new ModifyQuery(mapTable.table,writeQuery));

        field = writeField;
    }

    int getCoeff(PropertyMapImplement<?, T> implement) { return 0; }

    public Type getType() {
        SourceExpr calculateExpr = calculateSourceExpr(getMapKeys());
        return calculateExpr.getType(calculateExpr.getWhere());
    }

    protected Map<T, ValueClass> getMapClasses() {
        JoinQuery<T, String> query = new JoinQuery<T, String>(this);
        query.and(calculateSourceExpr(query.mapKeys).getWhere());
        return query.<T>getClassWhere(new ArrayList<String>()).getCommonParent(interfaces);
    }

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        JoinQuery<KeyField, Field> query = new JoinQuery<KeyField,Field>(mapTable.table);
        SourceExpr expr = calculateSourceExpr(BaseUtils.join(mapTable.mapKeys, query.mapKeys));
        query.properties.put(storedField,expr);
        query.and(expr.getWhere());
        return query.getClassWhere(Collections.singleton(storedField));
    }
}
