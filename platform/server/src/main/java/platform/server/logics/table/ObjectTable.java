package platform.server.logics.table;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.classes.SystemClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.type.ObjectType;
import platform.server.data.where.classes.ClassWhere;

// таблица в которой лежат объекты
public class ObjectTable extends DataTable {

    public KeyField key;
    public PropertyField objectClass;

    public ObjectTable(BaseClass baseClass) {
        super("objects");

        key = new KeyField("object", ObjectType.instance);
        keys = SetFact.singletonOrder(key);

        objectClass = new PropertyField("class", SystemClass.instance);
        properties = SetFact.singleton(objectClass);

        classes = new ClassWhere<KeyField>(key,baseClass.getUpSet());

        ImMap<Field, AndClassSet> valueClasses = MapFact.toMap(key,baseClass.getUpSet(),objectClass,SystemClass.instance);
        propertyClasses = MapFact.singleton(objectClass, new ClassWhere<Field>(valueClasses));
    }

}
