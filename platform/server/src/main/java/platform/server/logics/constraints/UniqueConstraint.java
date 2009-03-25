package platform.server.logics.constraints;

import platform.interop.Compare;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.DataSession;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.*;

// >= 0
class UniqueConstraint extends Constraint {

    public String check(DataSession session, Property property) throws SQLException {

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
        Map<PropertyInterface, KeyExpr> mapChange = new HashMap<PropertyInterface, KeyExpr>();
        Map<PropertyInterface, KeyExpr> mapPrev = new HashMap<PropertyInterface, KeyExpr>();
        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces) {
            mapChange.put(propertyInterface,changed.mapKeys.get(propertyInterface));
            mapPrev.put(propertyInterface,changed.mapKeys.get(mapPrevKeys.get(propertyInterface)));
        }

        JoinExpr changedExpr = session.propertyChanges.get(property).getExpr(mapChange,0);
        SourceExpr prevExpr = session.getSourceExpr(property,mapPrev, property.getUniversalInterface());

        // равны значения
        changed.and(new CompareWhere(prevExpr,changedExpr, Compare.EQUALS));
        // значения не NULL
        changed.and(changedExpr.getWhere());

        // не равны ключи
        Where orDiffKeys = Where.FALSE;

        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>) property.interfaces)
            orDiffKeys = orDiffKeys.or(new CompareWhere(mapChange.get(propertyInterface),mapPrev.get(propertyInterface), Compare.NOT_EQUALS));
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
