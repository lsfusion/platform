package platform.server.session;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.*;
import platform.server.data.where.classes.ClassWhere;
import platform.server.classes.CustomClass;
import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.logics.DataObject;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DataChangeTable extends ChangePropertyTable<ClassPropertyInterface,DataChangeTable> {

    public DataChangeTable(DataProperty property) {
        super("property",property);
    }

    public DataChangeTable(String iName, Map<KeyField, ClassPropertyInterface> iMapKeys, PropertyField iValue, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iMapKeys, iValue, iClasses, iPropertyClasses);
    }

    public DataChangeTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new DataChangeTable(name, mapKeys, value, iClasses, iPropertyClasses);
    }

    public void dropChanges(SQLSession session, DataProperty property, DataObject object, Collection<CustomClass> removeClasses) throws SQLException {
        for(ClassPropertyInterface propertyInterface : property.interfaces)
            if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass))
                session.deleteKeyRecords(this, Collections.singletonMap(BaseUtils.reverse(mapKeys).get(propertyInterface),(Integer)object.object));
        if(property.value instanceof CustomClass && removeClasses.contains((CustomClass)property.value)) {
            Query<KeyField,PropertyField> dropValues = new Query<KeyField, PropertyField>(this);
            platform.server.data.query.Join<PropertyField> dataJoin = joinAnd(dropValues.mapKeys);
            dropValues.and(dataJoin.getExpr(value).compare(object, Compare.EQUALS));
            dropValues.properties.put(value, Expr.NULL);
            session.updateRecords(new ModifyQuery(this,dropValues));
        }
    }

}
