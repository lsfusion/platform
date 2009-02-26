package platform.server.logics.constraints;

import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class ValueConstraint extends Constraint {

    ValueConstraint(int iInvalid) {
        invalid = iInvalid;
    }

    int invalid;

    public String check(DataSession session, Property property) throws SQLException {

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
