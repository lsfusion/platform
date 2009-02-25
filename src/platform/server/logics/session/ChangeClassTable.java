package platform.server.logics.session;

import platform.server.data.types.Type;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.Join;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.logics.classes.*;
import platform.server.logics.BusinessLogics;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.SQLException;

// хранит добавляение\удаление классов
public class ChangeClassTable extends ChangeTable {

    KeyField objectClass;
    public KeyField object;

    ChangeClassTable(String iTable) {
        super(iTable);

        object = new KeyField("object", Type.object);
        keys.add(object);

        objectClass = new KeyField("class",Type.system);
        keys.add(objectClass);
    }

    void changeClass(DataSession changeSession, Integer idObject, Collection<RemoteClass> Classes,boolean Drop) throws SQLException {

        for(RemoteClass change : Classes) {
            Map<KeyField,Integer> changeKeys = new HashMap<KeyField, Integer>();
            changeKeys.put(object,idObject);
            changeKeys.put(objectClass,change.ID);
            if(Drop) {
                if(!BusinessLogics.autoFillDB)
                    changeSession.deleteKeyRecords(this,changeKeys);
            } else
                changeSession.insertRecord(this,changeKeys,new HashMap<PropertyField, Object>());
        }
    }

    void dropSession(DataSession changeSession) throws SQLException {
        Map<KeyField,Integer> ValueKeys = new HashMap<KeyField, Integer>();
        changeSession.deleteKeyRecords(this,ValueKeys);
    }

    public JoinQuery<KeyField,PropertyField> getClassJoin(DataSession changeSession, RemoteClass changeClass) {

        Collection<KeyField> objectKeys = new ArrayList<KeyField>();
        objectKeys.add(object);
        JoinQuery<KeyField,PropertyField> classQuery = new JoinQuery<KeyField,PropertyField>(objectKeys);

        Join<KeyField,PropertyField> classJoin = new Join<KeyField,PropertyField>(this);
        classJoin.joins.put(object,classQuery.mapKeys.get(object));
        classJoin.joins.put(objectClass,objectClass.type.getExpr(changeClass.ID));
        classQuery.and(classJoin.inJoin);

        return classQuery;
    }

}
