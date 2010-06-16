package platform.server.data;

import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Map;
import java.util.Collection;

public class CustomSessionTable extends SessionTable<CustomSessionTable> {

    public CustomSessionTable(String name, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Collection<KeyField> keys, Collection<PropertyField> properties) {
        super(name, classes, propertyClasses, null);

        this.keys.addAll(keys);
        this.properties.addAll(properties);
    }

    public CustomSessionTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new CustomSessionTable(name, classes, propertyClasses, keys, properties);
    }
}
