package platform.server.logics.table;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.where.classes.ClassWhere;
import platform.server.classes.BaseClass;
import platform.server.classes.SystemClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.type.ObjectType;

import java.util.HashMap;
import java.util.Map;

// таблица в которой лежат объекты
public class ObjectTable extends Table {

    public KeyField key;
    public PropertyField objectClass;

    public ObjectTable(BaseClass baseClass) {
        super("objects");

        key = new KeyField("object", ObjectType.instance);
        keys.add(key);

        objectClass = new PropertyField("class", SystemClass.instance);
        properties.add(objectClass);

        classes = new ClassWhere<KeyField>(key,baseClass.getUpSet());

        Map<Field, AndClassSet> valueClasses = new HashMap<Field, AndClassSet>();
        valueClasses.put(key,baseClass.getUpSet());
        valueClasses.put(objectClass, SystemClass.instance);
        propertyClasses.put(objectClass,new ClassWhere<Field>(valueClasses));
    }

}
