package platform.server.view.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

import platform.server.data.types.Type;
import platform.server.data.KeyField;
import platform.server.data.SessionTable;
import platform.server.logics.session.DataSession;

// таблица куда виды складывают свои объекты
public class ViewTable extends SessionTable {
    public ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        objects = new ArrayList<KeyField>();
        for(Integer i=0;i<iObjects;i++) {
            KeyField objKeyField = new KeyField("object"+i, Type.object);
            objects.add(objKeyField);
            keys.add(objKeyField);
        }

        view = new KeyField("viewid",Type.system);
        keys.add(view);
    }

    List<KeyField> objects;
    KeyField view;

    void dropViewID(DataSession session,Integer viewID) throws SQLException {
        Map<KeyField,Integer> valueKeys = new HashMap<KeyField, Integer>();
        valueKeys.put(view,viewID);
        session.deleteKeyRecords(this,valueKeys);
    }
}
