package platform.server.view.form;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.classes.where.ClassWhere;

import java.util.HashMap;
import java.util.Map;

// таблица куда виды складывают свои объекты
public class ViewTable extends SessionTable<ViewTable> {

    public final Map<KeyField,ObjectImplement> mapKeys;

    public ViewTable(GroupObjectImplement group, int GID) {
        super("viewtable_"+(GID>=0?GID:"n"+(-GID)));

        mapKeys = new HashMap<KeyField, ObjectImplement>();
        for(ObjectImplement object : group) {
            KeyField objKeyField = new KeyField("object"+ object.sID, object.getType());
            mapKeys.put(objKeyField,object);
            keys.add(objKeyField);
        }
    }

    public ViewTable(String iName, Map<KeyField, ObjectImplement> iMapKeys, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iClasses, iPropertyClasses);

        mapKeys = iMapKeys;
        keys.addAll(mapKeys.keySet());
    }

    public ViewTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new ViewTable(name, mapKeys, iClasses, iPropertyClasses);
    }
}
