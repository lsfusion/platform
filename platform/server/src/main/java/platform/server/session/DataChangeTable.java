package platform.server.session;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.logics.DataObject;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DataChangeTable extends ChangePropertyTable<DataPropertyInterface,DataChangeTable> {

    public DataChangeTable(DataProperty property) {
        super("data",property);
    }

    public DataChangeTable(String iName, Map<KeyField, DataPropertyInterface> iMapKeys, PropertyField iValue, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iMapKeys, iValue, iClasses, iPropertyClasses);
    }

    public DataChangeTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new DataChangeTable(name, mapKeys, value, iClasses, iPropertyClasses);
    }

    public void dropChanges(SQLSession session, DataProperty property, DataObject object, Collection<CustomClass> removeClasses) throws SQLException {
        for(DataPropertyInterface propertyInterface : property.interfaces)
            if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass))
                session.deleteKeyRecords(this, Collections.singletonMap(BaseUtils.reverse(mapKeys).get(propertyInterface),(Integer)object.object));
        if(property.value instanceof CustomClass && removeClasses.contains((CustomClass)property.value)) {
            JoinQuery<KeyField,PropertyField> dropValues = new JoinQuery<KeyField, PropertyField>(this);
            Join dataJoin = joinAnd(dropValues.mapKeys);
            dropValues.and(dataJoin.getExpr(value).compare(new ValueExpr(object), Compare.EQUALS));
            dropValues.properties.put(value,CaseExpr.NULL);
            session.updateRecords(new ModifyQuery(this,dropValues));
        }
    }

}
