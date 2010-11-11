package platform.server.session;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;

import java.sql.SQLException;
import java.util.*;

public class DataChangeTable extends ChangePropertyTable<ClassPropertyInterface,DataChangeTable> {

    public DataChangeTable(DataProperty property) {
        super("property",property);
    }

    private DataChangeTable(String name, Map<KeyField, ClassPropertyInterface> mapKeys, PropertyField value, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, mapKeys, value, classes, propertyClasses, rows);
    }

    public DataChangeTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new DataChangeTable(name, mapKeys, value, classes, propertyClasses, rows);
    }

    public DataChangeTable dropChanges(SQLSession session, DataProperty property, DataObject object, Collection<CustomClass> removeClasses) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> dropRows = null;
        if(rows!=null)
            dropRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);

        for(ClassPropertyInterface propertyInterface : property.interfaces)
            if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass)) {
                KeyField mapField = BaseUtils.reverse(mapKeys).get(propertyInterface);
                if(dropRows!=null) {
                    Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = dropRows.entrySet().iterator();
                    while(iterator.hasNext())
                        if(iterator.next().getKey().get(mapField).equals(object))
                            iterator.remove();
                } else
                    session.deleteKeyRecords(this, Collections.singletonMap(mapField,object.object));
            }
        if(property.value instanceof CustomClass && removeClasses.contains((CustomClass)property.value))
            if(dropRows!=null) {
                Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = dropRows.entrySet().iterator();
                while(iterator.hasNext())
                    if(iterator.next().getValue().get(value).equals(object))
                        iterator.remove();
            } else {
                Query<KeyField,PropertyField> dropValues = new Query<KeyField, PropertyField>(this);
                platform.server.data.query.Join<PropertyField> dataJoin = joinAnd(dropValues.mapKeys);
                dropValues.and(dataJoin.getExpr(value).compare(object, Compare.EQUALS));
                dropValues.properties.put(value, Expr.NULL);
                session.updateRecords(new ModifyQuery(this,dropValues));
            }

        return createThis(classes,propertyClasses,dropRows);
    }

}
