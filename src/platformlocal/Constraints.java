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
    
    abstract String check(DataSession session, Property property) throws SQLException;

}

abstract class ValueConstraint extends Constraint {
    
    ValueConstraint(int iInvalid) {
        invalid = iInvalid;
    }

    int invalid;

    String check(DataSession session, Property property) throws SQLException {

        JoinQuery<PropertyInterface,String> changed = new JoinQuery<PropertyInterface,String>(property.interfaces);

        SourceExpr valueExpr = session.propertyChanges.get(property).getExpr(changed.mapKeys,0);
        // закинем условие на то что мы ищем
        changed.and(new CompareWhere(valueExpr, property.changeTable.value.type.getEmptyValueExpr(), invalid));
        changed.properties.put("value", valueExpr);

        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> result = changed.executeSelect(session);
        if(result.size()>0) {
            String resultString = "Ограничение на св-во "+ property.caption +" нарушено"+'\n';
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> row : result.entrySet()) {
                resultString += "   Объекты : ";
                for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces)
                    resultString += row.getKey().get(propertyInterface)+" ";
                resultString += "Значение : "+row.getValue().get("value") + '\n';
            }
            
            return resultString;
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
    
    String check(DataSession session, Property property) throws SQLException {
        
        // надо проверить для каждого что старых нету
        // изменения JOIN'им (ст. запрос FULL JOIN новый) ON изм. зн = новому зн. WHERE код изм. = код нов. и ключи не равны и зн. не null

        // ключи на самом деле состоят из 2-х частей - первые измененные (Property), 2-е - старые (из UpdateUnionQuery)
        // соответственно надо создать объекты
        Map<PropertyInterface,Object> mapPrevKeys = new HashMap<PropertyInterface, Object>();
        for(PropertyInterface Interface : (Collection<PropertyInterface>) property.interfaces)
            mapPrevKeys.put(Interface,new Object());

        List<Object> ChangePrevKeys = new ArrayList<Object>(property.interfaces);
        ChangePrevKeys.addAll(mapPrevKeys.values());
        JoinQuery<Object,String> changed = new JoinQuery<Object,String>(ChangePrevKeys);
        Map<PropertyInterface,KeyExpr> mapChange = new HashMap<PropertyInterface, KeyExpr>();
        Map<PropertyInterface,KeyExpr> mapPrev = new HashMap<PropertyInterface, KeyExpr>();
        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces) {
            mapChange.put(propertyInterface,changed.mapKeys.get(propertyInterface));
            mapPrev.put(propertyInterface,changed.mapKeys.get(mapPrevKeys.get(propertyInterface)));
        }

        JoinExpr changedExpr = session.propertyChanges.get(property).getExpr(mapChange,0);
        SourceExpr prevExpr = session.getSourceExpr(property,mapPrev, property.getUniversalInterface());

        // равны значения
        changed.and(new CompareWhere(prevExpr,changedExpr,CompareWhere.EQUALS));
        // значения не NULL
        changed.and(changedExpr.getWhere());

        // не равны ключи
        Where orDiffKeys = Where.FALSE;
        
        Map<PropertyInterface,String> keyFields = new HashMap();
        Integer keyNum = 0;
        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces)
            orDiffKeys = orDiffKeys.or(new CompareWhere(mapChange.get(propertyInterface),mapPrev.get(propertyInterface),CompareWhere.NOT_EQUALS));
        changed.and(orDiffKeys);
        changed.properties.put("value", changedExpr);

        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> result = changed.executeSelect(session);
        if(result.size()>0) {
            String resultString = "Уникальное ограничение на св-во "+ property.caption +" нарушено"+'\n';
            for(Map.Entry<Map<Object,Integer>,Map<String,Object>> row : result.entrySet()) {
                resultString += "   Объекты (1,2) : ";
                for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces)
                    resultString += row.getKey().get(propertyInterface)+","+row.getKey().get((mapPrevKeys.get(propertyInterface)))+" ";
                resultString += "Значение : "+row.getValue().get("value")+'\n';
            }
            
            return resultString;
        } else
            return null;
    }    
}
