package platform.server.view.form;

import platform.server.data.*;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.sql.SQLException;

// таблица куда виды складывают свои объекты
public class ViewTable extends SessionTable<ViewTable> {

    public final Map<KeyField,ObjectImplement> mapKeys;

    public ViewTable(GroupObjectImplement group, int GID) {
        super("viewtable_"+(GID>=0?GID:"n"+(-GID)));

        mapKeys = new HashMap<KeyField, ObjectImplement>();
        for(ObjectImplement object : group.objects) {
            KeyField objKeyField = new KeyField("object"+ object.sID, object.getType());
            mapKeys.put(objKeyField,object);
            keys.add(objKeyField);
        }
    }

    private ViewTable(String name, Map<KeyField, ObjectImplement> mapKeys, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);

        this.mapKeys = mapKeys;
        keys.addAll(this.mapKeys.keySet());
    }

    public ViewTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new ViewTable(name, mapKeys, classes, propertyClasses, rows);
    }

    // для rollback'а надо актуализирует базу, вообщем то можно через writeKeys было бы решить, но так быстрее
    public void rewrite(SQLSession session, Set<Map<ObjectImplement,DataObject>> writeRows) throws SQLException {
        if(rows==null) {
            session.deleteKeyRecords(this, new HashMap<KeyField, Object>());
            for(Map<ObjectImplement, DataObject> row : writeRows)
                session.insertRecord(this, BaseUtils.join(mapKeys,row),new HashMap<PropertyField, ObjectValue>());
        }
    }
}
