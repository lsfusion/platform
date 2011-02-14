package platform.server.integration;

import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;
import platform.server.session.SessionTableUsage;

import java.sql.SQLException;
import java.util.*;

/**
 * User: DAle
 * Date: 06.12.2010
 * Time: 14:54:28
 */

public class IntegrationService {
    private ImportTable table;
    private Collection<ImportProperty<?>> properties;
    private Collection<? extends ImportKey<?>> keys;
    private DataSession session;

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties) {
        this.session = session;
        this.table = table;
        this.properties = properties;
        this.keys = keys;
    }

    public void synchronize(boolean addNew, boolean updateExisting, boolean deleteOld) throws SQLException {
        Map<ImportKey, List<DataObject>> keyValueLists = new HashMap<ImportKey, List<DataObject>>();
        for (ImportKey<?> key : keys) {
            keyValueLists.put(key, new ArrayList<DataObject>());
        }
        for (ImportTable.Row row : table) {
            Map<ImportKeyInterface, DataObject> keyValues = new HashMap<ImportKeyInterface, DataObject>();
            boolean processRow = true;
            for (ImportKey<?> key : keys) {
                Object value = key.readValue(session, row);
                if (value != null) {
                    if (!updateExisting) {
                        processRow = false;
                        break;
                    }
                    keyValues.put(key, session.getDataObject(value, ObjectType.instance));
                } else {
                    if (!addNew) {
                        processRow = false;
                        break;
                    } else {
                        DataObject newObject = session.addObject(key.getCustomClass(), session.modifier);
//                        key.writeValue(session, row, newObject);
                        keyValues.put(key, newObject);
                    }
                }
                keyValueLists.get(key).add(keyValues.get(key));
            }

            if (processRow) {
                for (ImportProperty<?> property : properties) {
                    DataObject dataObject = property.getImportField().getDataObject(row);
                    Object value = (dataObject == null ? null : dataObject.object);

                    if (property.getConverter() != null) {
                        value = property.convertValue(session, keyValues);
                    }

                    property.writeValue(session, keyValues, value);
                }
            }
        }

        if (deleteOld) {
            deleteOldObjects(keyValueLists);
        }
    }

    private void deleteOldObjects(Map<ImportKey, List<DataObject>> keyValueLists) throws SQLException {
        for (final ImportKey<?> key : keys) {
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
}
