package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Map;

public class AddClassTable extends ChangeClassTable<AddClassTable> {

    public AddClassTable(int classID) {
        super("addchange",classID);
    }

    public AddClassTable(String name, KeyField object,ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, object, classes, propertyClasses, rows);
    }

    public AddClassTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new AddClassTable(name,object, classes, propertyClasses, rows);
    }
}
