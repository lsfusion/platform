/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author ME2
 */

// constraint
abstract class Constraint {
    
    abstract String Check(DataSession Session, ObjectProperty Property) throws SQLException;

}

abstract class ValueConstraint extends Constraint {
    
    ValueConstraint(int iInvalid) {Invalid=iInvalid;}

    int Invalid;

    String Check(DataSession Session, ObjectProperty Property) throws SQLException {

        JoinQuery<PropertyInterface,String> Changed = new JoinQuery<PropertyInterface,String>(Property.Interfaces);

        SourceExpr ValueExpr = Property.getChangedSourceExpr(Changed.MapKeys,Session,0);
        // закинем условие на то что мы ищем
        Changed.Wheres.add(new FieldExprCompareWhere(ValueExpr,(Property.ChangeTable.Value.Type.equals("integer")?0:""),Invalid));
        Changed.Properties.put("value",ValueExpr);

        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> Result = Changed.executeSelect(Session);
        if(Result.size()>0) {
            String ResultString = "Ограничение на св-во "+Property.OutName+" нарушено"+'\n';
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row : Result.entrySet()) {
                ResultString += "   Объекты : ";
                for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
                    ResultString += Row.getKey().get(Interface)+" ";
                ResultString += "Значение : "+Row.getValue().get("value") + '\n';
            }
            
            return ResultString;
        } else
            return null;
    }
}

// != 0 или !="      "
class EmptyConstraint extends ValueConstraint {
    EmptyConstraint() {super(5);}
}

// <= 0 или <= ''
class NotEmptyConstraint extends ValueConstraint {
    NotEmptyConstraint() {super(4);}
}

// < 0
class PositiveConstraint extends ValueConstraint {
    PositiveConstraint() {super(2);}
}

// >= 0
class UniqueConstraint extends Constraint {
    
    String Check(DataSession Session, ObjectProperty Property) throws SQLException {
        
        // надо проверить для каждого что старых нету
        // изменения JOIN'им (ст. запрос FULL JOIN новый) ON изм. зн = новому зн. WHERE код изм. = код нов. и ключи не равны и зн. не null

        // ключи на самом деле состоят из 2-х частей - первые измененные (Property), 2-е - старые (из UpdateUnionQuery)
        // соответственно надо создать объекты
        JoinQuery<Object,String> Changed = new JoinQuery<Object,String>();
        Map<PropertyInterface,SourceExpr> MapChange = new HashMap();
        Map<PropertyInterface,SourceExpr> MapPrev = new HashMap();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            MapChange.put(Interface,Changed.addKey(new Object()));
            MapPrev.put(Interface,Changed.addKey(new Object()));
        }

        SourceExpr ChangedExpr = Property.getChangedSourceExpr(MapChange,Session,0);
        SourceExpr PrevExpr = Property.getUpdatedSourceExpr(MapPrev,Session,true);

        // равны значения
        Changed.Wheres.add(new FieldExprCompareWhere(ChangedExpr,PrevExpr,0));
        // значения не NULL
        Changed.Wheres.add(new SourceIsNullWhere(ChangedExpr,true));

        // не равны ключи
        SourceWhere OrDiffKeys = null;
        
        Map<PropertyInterface,String> KeyFields = new HashMap();
        Integer KeyNum = 0;
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            // не равны ключи
            SourceWhere KeyDiff = new FieldExprCompareWhere(MapChange.get(Interface),MapPrev.get(Interface),5);
            if(OrDiffKeys==null)
                OrDiffKeys = KeyDiff;
            else
                OrDiffKeys = new FieldOPWhere(KeyDiff,OrDiffKeys,false);
        }
        Changed.Wheres.add(OrDiffKeys);
        Changed.Properties.put("value",ChangedExpr);

        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> Result = Changed.executeSelect(Session);
        if(Result.size()>0) {
            String ResultString = "Уникальное ограничение на св-во "+Property.OutName+" нарушено"+'\n';
            for(Map.Entry<Map<Object,Integer>,Map<String,Object>> Row : Result.entrySet()) {
                ResultString += "   Объекты (1,2) : ";
                for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
                    ResultString += Row.getKey().get(((KeyExpr<Object>)MapChange.get(Interface)).Key)+","+Row.getKey().get(((KeyExpr<Object>)MapPrev.get(Interface)).Key)+" ";
                ResultString += "Значение : "+Row.getValue().get("value")+'\n';
            }
            
            return ResultString;
        } else
            return null;
    }    
}
