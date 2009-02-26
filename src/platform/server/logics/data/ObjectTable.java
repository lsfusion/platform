package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.wheres.InListWhere;
import platform.server.data.types.Type;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// таблица в которой лежат объекты
public class ObjectTable extends Table {

    public KeyField key;
    public PropertyField objectClass;

    ObjectTable() {
        super("objects");
        key = new KeyField("object", Type.object);
        keys.add(key);
        objectClass = new PropertyField("class",Type.system);
        properties.add(objectClass);
    }

    public Integer getClassID(DataSession session,Integer idObject) throws SQLException {
        if(idObject==null) return null;

        JoinQuery<Object,String> query = new JoinQuery<Object,String>(new ArrayList<Object>());
        Join<KeyField, PropertyField> joinTable = new Join<KeyField,PropertyField>(this);
        joinTable.joins.put(key,key.type.getExpr(idObject));
        query.and(joinTable.inJoin);
        query.properties.put("classid", joinTable.exprs.get(objectClass));
        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> result = query.executeSelect(session);
        if(result.size()>0)
            return (Integer)result.values().iterator().next().get("classid");
        else
            return null;
    }

    public JoinQuery<KeyField,PropertyField> getClassJoin(RemoteClass changeClass) {

        Collection<Integer> idSet = new ArrayList<Integer>();
        Collection<RemoteClass> classSet = new ArrayList<RemoteClass>();
        changeClass.fillChilds(classSet);
        for(RemoteClass childClass : classSet)
            idSet.add(childClass.ID);

        JoinQuery<KeyField,PropertyField> classQuery = new JoinQuery<KeyField,PropertyField>(keys);
        classQuery.and(new InListWhere((new Join<KeyField,PropertyField>(this,classQuery)).exprs.get(objectClass), idSet));

        return classQuery;
    }
}
