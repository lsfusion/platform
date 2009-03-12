package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.logics.data.MapKeysTable;
import platform.server.logics.session.ChangeValue;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    protected AggregateProperty(String iSID,Collection<T> iInterfaces, TableFactory iTableFactory) {
        super(iSID,iInterfaces,iTableFactory);
    }

    // расчитывает выражение
    abstract SourceExpr calculateSourceExpr(Map<T,? extends SourceExpr> joinImplement, InterfaceClassSet<T> joinClasses);

    Object dropZero(Object Value) {
        if(Value instanceof Integer && Value.equals(0)) return null;
        if(Value instanceof Long && ((Long)Value).intValue()==0) return null;
        if(Value instanceof Double && ((Double)Value).intValue()==0) return null;
        if(Value instanceof Boolean && !((Boolean)Value)) return null;
        return Value;
    }

    // проверяет аггрегацию для отладки
    public boolean checkAggregation(DataSession Session,String Caption) throws SQLException {
        JoinQuery<T, String> AggrSelect;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<T, Integer>, Map<String, Object>> AggrResult = AggrSelect.executeSelect(Session);
        tableFactory.reCalculateAggr = true;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("RECALCULATE - "+caption);
            AggrSelect.outSelect(Session);
        }*/
//        if(BusinessLogics.ChangeDBIteration==13 && caption.equals("Посл. дата изм. парам.")) {
//            System.out.println("RECALCULATE - "+caption);
//            AggrSelect.outSelect(Session);
//        }

        LinkedHashMap<Map<T, Integer>, Map<String, Object>> CalcResult = AggrSelect.executeSelect(Session);
        tableFactory.reCalculateAggr = false;

        Iterator<Map.Entry<Map<T,Integer>,Map<String,Object>>> i = AggrResult.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Map<T,Integer>,Map<String,Object>> Row = i.next();
            Map<T, Integer> RowKey = Row.getKey();
            Object RowValue = dropZero(Row.getValue().get("value"));
            Map<String,Object> CalcRow = CalcResult.get(RowKey);
            Object CalcValue = (CalcRow==null?null:dropZero(CalcRow.get("value")));
            if(RowValue==CalcValue || (RowValue!=null && RowValue.equals(CalcValue))) {
                i.remove();
                CalcResult.remove(RowKey);
            }
        }
        // вычистим и отсюда 0
        i = CalcResult.entrySet().iterator();
        while(i.hasNext()) {
            if(dropZero(i.next().getValue().get("value"))==null)
                i.remove();
        }

        if(CalcResult.size()>0 || AggrResult.size()>0) {
            System.out.println("----CheckAggregations "+Caption+"-----");
            System.out.println("----Aggr-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : AggrResult.entrySet())
                System.out.println(Row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : CalcResult.entrySet())
                System.out.println(Row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    public void recalculateAggregation(DataSession session) throws SQLException {
        PropertyField writeField = field;
        field = null;

        JoinQuery<KeyField, PropertyField> writeQuery = new JoinQuery<KeyField, PropertyField>(mapTable.table.keys);
        SourceExpr recalculateExpr = getSourceExpr(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), writeQuery.mapKeys), getClassSet(ClassSet.universal));
        writeQuery.properties.put(writeField, recalculateExpr);
        writeQuery.and(new Join<KeyField, PropertyField>(mapTable.table, writeQuery).inJoin.or(recalculateExpr.getWhere()));
        session.modifyRecords(new ModifyQuery(mapTable.table,writeQuery));

        field = writeField;
    }

    List<PropertyMapImplement<PropertyInterface, T>> getImplements(Map<T, ObjectValue> keys, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        return new ArrayList<PropertyMapImplement<PropertyInterface, T>>();
    }

    int getCoeff(PropertyMapImplement<?, T> implement) { return 0; }

    PropertyMapImplement<?,T> getChangeImplement(Map<T, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        List<PropertyMapImplement<PropertyInterface, T>> Implements = getImplements(Keys, securityPolicy);
        for(int i=Implements.size()-1;i>=0;i--)
            if(Implements.get(i).mapGetChangeProperty(null, Keys, 0, securityPolicy)!=null && (securityPolicy == null || securityPolicy.checkPermission(Implements.get(i).property)))
                return Implements.get(i);
        return null;
    }

    public ChangeValue getChangeProperty(DataSession Session, Map<T, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        PropertyMapImplement<?,T> Implement = getChangeImplement(Keys, securityPolicy);
        if(Implement==null) return null;
        return Implement.mapGetChangeProperty(Session,Keys,getCoeff(Implement)*Coeff, securityPolicy);
    }

    public void changeProperty(Map<T, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        PropertyMapImplement<?,T> operand = getChangeImplement(Keys, securityPolicy);
        if (operand != null)
            operand.mapChangeProperty(Keys, NewValue, externalID, Session, securityPolicy);
    }

}
