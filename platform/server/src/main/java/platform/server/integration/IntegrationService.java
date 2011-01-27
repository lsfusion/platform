package platform.server.integration;

import platform.base.BaseUtils;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.SessionTableUsage;

import java.sql.SQLException;
import java.util.*;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:54:28
 */

public class IntegrationService<P extends PropertyInterface> {
    private ImportTable table;
    private Map<ImportField, PropertyImplement<ImportKey<P>, P>> properties;
    private Collection<ImportKey<P>> keys;
    private DataSession session;

    public IntegrationService(DataSession session, ImportTable table, Collection<ImportKey<P>> keys,
                              Map<ImportField, PropertyImplement<ImportKey<P>, P>> properties) {
        this.session = session;
        this.table = table;
        this.properties = properties;
        this.keys = keys;
    }

    public void synchronize(boolean addNew, boolean updateExisting, boolean deleteOld) throws SQLException {
        Map<ImportKey, List<DataObject>> keyValueLists = new HashMap<ImportKey, List<DataObject>>();
        for (ImportKey<P> key : keys) {
            keyValueLists.put(key, new ArrayList<DataObject>());
        }
        for (ImportTable.Row row : table) {
            Map<ImportKey, DataObject> keyValues = new HashMap<ImportKey, DataObject>();
            boolean processRow = true;
            for (ImportKey<P> key : keys) {
                Map<P, DataObject> map = mapObjects(key, row);
                Object value = key.getProperty().read(session.sql, map, session.modifier, session.env);
                if (value != null) {
                    if (!updateExisting) {
                        processRow = false;
                        break;
                    }
                    keyValues.put(key, new DataObject(value, key.getCustomClass()));
                } else {
                    if (!addNew) {
                        processRow = false;
                        break;
                    } else {
                        DataObject newObject = session.addObject(key.getCustomClass(), session.modifier);
                        key.getProperty().execute(map, session, newObject.object, session.modifier);
                        keyValues.put(key, newObject);
                    }
                }
                keyValueLists.get(key).add(keyValues.get(key));
            }

            if (processRow) {
                for (Map.Entry<ImportField, PropertyImplement<ImportKey<P>, P>> entry : properties.entrySet()) {
                    Map<P, DataObject> mapping = BaseUtils.join(entry.getValue().mapping, keyValues);
                    entry.getValue().property.execute(mapping, session, row.getValue(entry.getKey()), session.modifier);
                }
            }
        }

        if (deleteOld) {
            deleteOldObjects(keyValueLists);
        }
    }

    private void deleteOldObjects(Map<ImportKey, List<DataObject>> keyValueLists) throws SQLException {
        for (final ImportKey<P> key : keys) {
            SessionTableUsage<String, Object> table = new SessionTableUsage<String, Object>(Arrays.asList("key"), new ArrayList<Object>(),
                    new Type.Getter<String>() {
                        public Type getType(String k) {
                            return key.getCustomClass().getType();
                        }
                    },
                    new Type.Getter<Object>() {
                        public Type getType(Object o) {return null;}
                    });

            for (DataObject keyValue : keyValueLists.get(key)) {
                table.insertRecord(session.sql, Collections.singletonMap("key", keyValue), new HashMap<Object, ObjectValue>(), false);
            }

            Query<String, Object> query = new Query<String, Object>(Collections.singletonList("key"));
            query.and(query.mapKeys.get("key").isClass(key.getCustomClass().getUpSet()));
            query.and(table.getWhere(query.mapKeys).not());

            Set<Map<String, Object>> keyMaps = query.execute(session.sql).keySet();
            for (Map<String, Object> keyMap : keyMaps) {
                session.changeClass(new DataObject(keyMap.get("key"), key.getCustomClass()), null);
            }
        }
    }

    private Map<P, DataObject> mapObjects(ImportKey<P> key, ImportTable.Row row) {
        Map<P, DataObject> map = new HashMap<P, DataObject>();
        for (Map.Entry<P, ImportField> entry : key.getMapping().entrySet()) {
            DataObject obj = new DataObject(row.getValue(entry.getValue()), entry.getValue().getFieldClass());
            map.put(entry.getKey(), obj);
        }
        return map;
    }
}
