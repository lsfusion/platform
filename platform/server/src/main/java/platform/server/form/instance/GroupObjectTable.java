package platform.server.form.instance;

import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.base.BaseUtils;

import java.util.*;
import java.sql.SQLException;

// нужен в общем то потому что в SessionTable нельзя помещать Map'ы так как будет Memory Leak, хотя в принципе и какую-то свою семантику имеет
public class GroupObjectTable {

    public final Map<KeyField, ObjectInstance> mapKeys;
    public final CustomSessionTable table;

    public GroupObjectTable(GroupObjectInstance group, String prefix, int GID) {
        mapKeys = new HashMap<KeyField, ObjectInstance>();

        List<KeyField> keys = new ArrayList<KeyField>();
        for(ObjectInstance object : GroupObjectInstance.getObjects(group.getUpTreeGroups())) {
            KeyField objKeyField = new KeyField("object"+ object.getsID(), object.getType());
            mapKeys.put(objKeyField,object);
            keys.add(objKeyField);
        }

        table = new CustomSessionTable("viewtable"+prefix+"_"+(GID>=0?GID:"n"+(-GID)), keys);
    }

    public GroupObjectTable(Map<KeyField, ObjectInstance> mapKeys, CustomSessionTable table) {
        this.mapKeys = mapKeys;
        this.table = table;
    }

    public GroupObjectTable writeKeys(SQLSession session,Collection<Map<ObjectInstance,DataObject>> writeRows) throws SQLException {
        return new GroupObjectTable(mapKeys, table.writeKeys(session, BaseUtils.joinCol(mapKeys, writeRows)));
    }

    public void rewrite(SQLSession session, Collection<Map<ObjectInstance,DataObject>> writeRows) throws SQLException {
        table.rewrite(session, BaseUtils.joinCol(mapKeys, writeRows));
    }

    public GroupObjectTable insertRecord(SQLSession session, Map<ObjectInstance, DataObject> keyFields, boolean update) throws SQLException {
        return new GroupObjectTable(mapKeys, table.insertRecord(session, BaseUtils.join(mapKeys, keyFields),new HashMap<PropertyField, ObjectValue>(), update));
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> joinExprs) {
        return table.join(BaseUtils.join(mapKeys, joinExprs)).getWhere();
    }

}
