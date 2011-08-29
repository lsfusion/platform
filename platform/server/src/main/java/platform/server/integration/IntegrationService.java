package platform.server.integration;

import platform.server.classes.IntegerClass;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

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
        synchronize(addNew, updateExisting, deleteOld, false);
    }

    public void synchronize(boolean addNew, boolean updateExisting, boolean deleteOld, boolean replaceNull) throws SQLException {
        synchronize(addNew, updateExisting, deleteOld, replaceNull, true);
    }

    public void synchronize(boolean addNew, boolean updateExisting, boolean deleteOld, boolean replaceNull, boolean replaceEqual) throws SQLException {
        SingleKeyTableUsage<ImportField> importTable = new SingleKeyTableUsage<ImportField>(IntegerClass.instance, table.fields, ImportField.typeGetter);

        int counter = 0;
        for (Iterator iterator = table.iterator(); iterator.hasNext();) {
            PlainDataTable.Row row = (PlainDataTable.Row) iterator.next();
            Map<ImportField, ObjectValue> insertRow = new HashMap<ImportField, ObjectValue>();
            for (ImportField field : table.fields)
                insertRow.put(field, ObjectValue.getValue(row.getValue(field), field.getFieldClass()));
            importTable.insertRecord(session.sql, new DataObject(counter++), insertRow, false, !iterator.hasNext());
        }

        // приходится через addKeys, так как synchronize сам не может resolv'ить сессию на добавление
        Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys = new HashMap<ImportKey<?>, SinglePropertyTableUsage<?>>();
        for (ImportKey<?> key : keys)
            addedKeys.put(key, key.synchronize(session, importTable));

        MapDataChanges<PropertyInterface> propertyChanges = new MapDataChanges<PropertyInterface>();
        for (ImportProperty<?> property : properties)
            propertyChanges = propertyChanges.add((MapDataChanges<PropertyInterface>) property.synchronize(session, importTable, addedKeys, replaceNull, replaceEqual));
        session.execute(propertyChanges, null, null);
    }

/*    private void deleteOldObjects(Map<ImportKey, List<DataObject>> keyValueLists) throws SQLException {
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

            for (Iterator<DataObject> iterator = keyValueLists.get(key).iterator(); iterator.hasNext();) {
                DataObject keyValue = iterator.next();
                table.insertRecord(session.sql, Collections.singletonMap("key", keyValue), new HashMap<Object, ObjectValue>(), false, !iterator.hasNext());
            }

            Query<String, Object> query = new Query<String, Object>(Collections.singletonList("key"));
            query.and(query.mapKeys.get("key").isClass(key.getCustomClass().getUpSet()));
            query.and(table.getWhere(query.mapKeys).not());

            Set<Map<String, Object>> keyMaps = query.execute(session.sql).keySet();
            for (Map<String, Object> keyMap : keyMaps) {
                session.changeClass(new DataObject(keyMap.get("key"), key.getCustomClass()), null);
            }
        }
    }*/
}
