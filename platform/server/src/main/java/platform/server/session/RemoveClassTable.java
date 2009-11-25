package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.where.classes.ClassWhere;

import java.util.Map;

public class RemoveClassTable extends ChangeClassTable<RemoveClassTable> {

    public RemoveClassTable(int classID) {
        super("removechange",classID);
    }

    public RemoveClassTable(String iName,KeyField iObject,ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iObject, iClasses, iPropertyClasses);
    }

    public RemoveClassTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new RemoveClassTable(name,object,iClasses, iPropertyClasses);
    }
}
