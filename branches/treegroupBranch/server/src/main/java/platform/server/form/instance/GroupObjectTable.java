package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.server.data.*;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// таблица куда виды складывают свои объекты
public class GroupObjectTable extends SessionTable<GroupObjectTable> {

    public final Map<KeyField, ObjectInstance> mapKeys;

    public GroupObjectTable(GroupObjectInstance group, int GID) {
        super("viewtable_"+(GID>=0?GID:"n"+(-GID)));

        mapKeys = new HashMap<KeyField, ObjectInstance>();
        for(ObjectInstance object : group.objects) {
            KeyField objKeyField = new KeyField("object"+ object.getsID(), object.getType());
            mapKeys.put(objKeyField,object);
            keys.add(objKeyField);
        }
    }

    private GroupObjectTable(String name, Map<KeyField, ObjectInstance> mapKeys, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);

        this.mapKeys = mapKeys;
        keys.addAll(this.mapKeys.keySet());
    }

    public GroupObjectTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new GroupObjectTable(name, mapKeys, classes, propertyClasses, rows);
    }

    // для rollback'а надо актуализирует базу, вообщем то можно через writeKeys было бы решить, но так быстрее
    public void rewrite(SQLSession session, Set<Map<ObjectInstance,DataObject>> writeRows) throws SQLException {
        if(rows==null) {
            session.deleteKeyRecords(this, new HashMap<KeyField, Object>());
            for(Map<ObjectInstance, DataObject> row : writeRows)
                session.insertRecord(this, BaseUtils.join(mapKeys,row),new HashMap<PropertyField, ObjectValue>());
        }
    }
}
