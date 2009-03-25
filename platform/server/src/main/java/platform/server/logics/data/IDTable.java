package platform.server.logics.data;

import platform.interop.Compare;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.types.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// таблица счетчика sID
public class IDTable extends Table {
    KeyField key;
    PropertyField value;

    IDTable() {
        super("idtable");
        key = new KeyField("id", Type.system);
        keys.add(key);

        value = new PropertyField("value",Type.system);
        properties.add(value);
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;

    static List<Integer> getCounters() {
        List<Integer> result = new ArrayList<Integer>();
        result.add(OBJECT);
        result.add(FORM);
        return result;
    }

    public Integer generateID(DataSession dataSession, int idType) throws SQLException {

        if(BusinessLogics.autoFillDB) return BusinessLogics.autoIDCounter++;
        // читаем
        JoinQuery<KeyField, PropertyField> query = new JoinQuery<KeyField, PropertyField>(keys);
        Join<KeyField,PropertyField> joinTable = new Join<KeyField, PropertyField>(this);
        joinTable.joins.put(key,query.mapKeys.get(key));
        query.and(joinTable.inJoin);
        query.properties.put(value, joinTable.exprs.get(value));

        query.and(new CompareWhere(query.mapKeys.get(key),Type.object.getExpr(idType), Compare.EQUALS));

        Integer freeID = (Integer) query.executeSelect(dataSession).values().iterator().next().get(value);

        // замещаем
        reserveID(dataSession, idType, freeID);
        return freeID+1;
    }

    public void reserveID(DataSession session, int idType, Integer ID) throws SQLException {
        JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(keys);
        updateQuery.putKeyWhere(Collections.singletonMap(key,idType));
        updateQuery.properties.put(value,value.type.getExpr(ID+1));
        session.updateRecords(new ModifyQuery(this,updateQuery));
    }
}
