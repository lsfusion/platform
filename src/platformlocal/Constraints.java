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
    
    abstract String Check(DataSession Session, Property Property) throws SQLException;

}

abstract class ValueConstraint extends Constraint {
    
    ValueConstraint(int iInvalid) {Invalid=iInvalid;}

    int Invalid;

    String Check(DataSession Session, Property Property) throws SQLException {

        JoinQuery<PropertyInterface,String> Changed = new JoinQuery<PropertyInterface,String>(Property.Interfaces);

        SourceExpr ValueExpr = Session.PropertyChanges.get(Property).getExpr(Changed.MapKeys,0);
        // закинем условие на то что мы ищем
        Changed.and(new CompareWhere(ValueExpr,Property.ChangeTable.Value.Type.getEmptyValueExpr(),Invalid));
        Changed.Properties.put("value", ValueExpr);

        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> Result = Changed.compile().executeSelect(Session, false);
        if(Result.size()>0) {
            String ResultString = "Ограничение на св-во "+Property.caption +" нарушено"+'\n';
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
    EmptyConstraint() {super(CompareWhere.NOT_EQUALS);}
}

// <= 0 или <= ''
class NotEmptyConstraint extends ValueConstraint {
    NotEmptyConstraint() {super(CompareWhere.LESS_EQUALS);}
}

// < 0
class PositiveConstraint extends ValueConstraint {
    PositiveConstraint() {super(CompareWhere.LESS);}
}

// >= 0
class UniqueConstraint extends Constraint {
    
    String Check(DataSession Session, Property Property) throws SQLException {
        
        // надо проверить для каждого что старых нету
        // изменения JOIN'им (ст. запрос FULL JOIN новый) ON изм. зн = новому зн. WHERE код изм. = код нов. и ключи не равны и зн. не null

        // ключи на самом деле состоят из 2-х частей - первые измененные (Property), 2-е - старые (из UpdateUnionQuery)
        // соответственно надо создать объекты
        Map<PropertyInterface,Object> MapPrevKeys = new HashMap<PropertyInterface, Object>();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
            MapPrevKeys.put(Interface,new Object());

        List<Object> ChangePrevKeys = new ArrayList<Object>(Property.Interfaces);
        ChangePrevKeys.addAll(MapPrevKeys.values());
        JoinQuery<Object,String> Changed = new JoinQuery<Object,String>(ChangePrevKeys);
        Map<PropertyInterface,KeyExpr> MapChange = new HashMap<PropertyInterface, KeyExpr>();
        Map<PropertyInterface,KeyExpr> MapPrev = new HashMap<PropertyInterface, KeyExpr>();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces) {
            MapChange.put(Interface,Changed.MapKeys.get(Interface));
            MapPrev.put(Interface,Changed.MapKeys.get(MapPrevKeys.get(Interface)));
        }

        JoinExpr ChangedExpr = Session.PropertyChanges.get(Property).getExpr(MapChange,0);
        SourceExpr PrevExpr = Session.getSourceExpr(Property,MapPrev,Property.getUniversalInterface());

        // равны значения
        Changed.and(new CompareWhere(PrevExpr,ChangedExpr,CompareWhere.EQUALS));
        // значения не NULL
        Changed.and(ChangedExpr.getWhere());

        // не равны ключи
        OuterWhere OrDiffKeys = new OuterWhere();
        
        Map<PropertyInterface,String> KeyFields = new HashMap();
        Integer KeyNum = 0;
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Property.Interfaces)
            OrDiffKeys.out(new CompareWhere(MapChange.get(Interface),MapPrev.get(Interface),CompareWhere.NOT_EQUALS));
        Changed.and(OrDiffKeys);
        Changed.Properties.put("value", ChangedExpr);

        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> Result = Changed.compile().executeSelect(Session, false);
        if(Result.size()>0) {
            String ResultString = "Уникальное ограничение на св-во "+Property.caption +" нарушено"+'\n';
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
