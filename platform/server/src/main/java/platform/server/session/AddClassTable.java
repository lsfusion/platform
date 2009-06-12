package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.where.ClassWhere;

import java.util.Map;

public class AddClassTable extends ChangeClassTable<AddClassTable> {

    public AddClassTable(int classID) {
        super("addchange",classID);
    }

    public AddClassTable(String iName, KeyField iObject,ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iObject, iClasses, iPropertyClasses);
    }

    public AddClassTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new AddClassTable(name,object,iClasses,iPropertyClasses);
    }
}
