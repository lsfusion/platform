package platform.server.logics.data;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.SystemClass;
import platform.server.data.classes.where.AndClassWhere;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.types.ObjectType;

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

        AndClassWhere<Field> valueClasses = new AndClassWhere<Field>();
        valueClasses.add(key,baseClass.getUpSet());
        valueClasses.add(objectClass, SystemClass.instance);
        propertyClasses.put(objectClass,new ClassWhere<Field>(valueClasses));
    }

}
