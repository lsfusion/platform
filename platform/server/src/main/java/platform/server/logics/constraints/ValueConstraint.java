package platform.server.logics.constraints;

import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.where.WhereBuilder;
import platform.interop.Compare;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class ValueConstraint extends Constraint {

    ValueConstraint(Compare iInvalid) {
        invalid = iInvalid;
    }

    Compare invalid;

    public <P extends PropertyInterface> String check(DataSession session, Property<P> property) throws SQLException {

        JoinQuery<P,String> changed = new JoinQuery<P,String>(property);

        WhereBuilder changedWhere = new WhereBuilder();
        SourceExpr valueExpr = property.getSourceExpr(changed.mapKeys,session.changes,session,changedWhere);
        // закинем условие на то что мы ищем
        changed.and(valueExpr.compare(new ValueExpr(property.getType().getEmptyValueExpr()), invalid));
        changed.and(changedWhere.toWhere());
        changed.properties.put("value", valueExpr);

        LinkedHashMap<Map<P, Object>, Map<String, Object>> result = changed.executeSelect(session);
        if(result.size()>0) {
            String resultString = "Ограничение на св-во "+ property.caption +" нарушено"+'\n';
            for(Map.Entry<Map<P,Object>,Map<String,Object>> row : result.entrySet()) {
                resultString += "   Объекты : ";
                for(P propertyInterface : property.interfaces)
                    resultString += row.getKey().get(propertyInterface)+" ";
                resultString += "Значение : "+row.getValue().get("value") + '\n';
            }

            return resultString;
        } else
            return null;
    }
}
