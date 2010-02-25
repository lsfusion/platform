package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Map;

public class RemoveClassTable extends ChangeClassTable<RemoveClassTable> {

    public RemoveClassTable(int classID) {
        super("removechange",classID);
    }

    public RemoveClassTable(String name,KeyField object,ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, object, classes, propertyClasses, rows);
    }

    public RemoveClassTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new RemoveClassTable(name,object, classes, propertyClasses, rows);
    }
}
