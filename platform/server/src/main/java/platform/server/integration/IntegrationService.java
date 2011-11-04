package platform.server.integration;

import platform.server.Message;
import platform.server.classes.IntegerClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyImplement;
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
    private Collection<ImportDelete> deletes;

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties) {
        this(session, table, keys, properties, null);
    }

    public IntegrationService(DataSession session, ImportTable table, Collection<? extends ImportKey<?>> keys,
                              Collection<ImportProperty<?>> properties, Collection<ImportDelete> deletes) {
        this.session = session;
        this.table = table;
        this.properties = properties;
        this.keys = keys;
        this.deletes = deletes;
    }

    public SessionTableUsage<String, ImportField> synchronize() throws SQLException {
        return synchronize(false);
    }

    public SessionTableUsage<String, ImportField> synchronize(boolean replaceNull) throws SQLException {
        return synchronize(replaceNull, true);
    }

    @Message("message.synchronize")
    public SessionTableUsage<String, ImportField> synchronize(boolean replaceNull, boolean replaceEqual) throws SQLException {
        SingleKeyTableUsage<ImportField> importTable = new SingleKeyTableUsage<ImportField>(IntegerClass.instance, table.fields, ImportField.typeGetter);

        int counter = 0;
        for (Iterator iterator = table.iterator(); iterator.hasNext();) {
            PlainDataTable.Row row = (PlainDataTable.Row) iterator.next();
            Map<ImportField, ObjectValue> insertRow = new HashMap<ImportField, ObjectValue>();
            for (ImportField field : table.fields)
                insertRow.put(field, ObjectValue.getValue(row.getValue(field), field.getFieldClass()));
            importTable.insertRecord(session.sql, new DataObject(counter++), insertRow, false, !iterator.hasNext());
        }

        if (deletes != null) {
            deleteObjects(importTable);
        }

        // приходится через addKeys, так как synchronize сам не может resolv'ить сессию на добавление
        Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys = new HashMap<ImportKey<?>, SinglePropertyTableUsage<?>>();
        for (ImportKey<?> key : keys)
            addedKeys.put(key, key.synchronize(session, importTable));

        MapDataChanges<PropertyInterface> propertyChanges = new MapDataChanges<PropertyInterface>();
        for (ImportProperty<?> property : properties)
            propertyChanges = propertyChanges.add((MapDataChanges<PropertyInterface>) property.synchronize(session, importTable, addedKeys, replaceNull, replaceEqual));
        session.execute(propertyChanges, null, null);

        return importTable;
    }

    private <P extends PropertyInterface> void deleteObjects(SingleKeyTableUsage<ImportField> importTable) throws SQLException {
        for (ImportDelete delete : deletes) {
            KeyExpr keyExpr = new KeyExpr("key");
            Query<String, String> query = new Query(Collections.singletonMap("key", keyExpr));

            // выражения для полей в импортируемой таблице
            Map<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();

            // фильтруем только те, которых нету в ImportTable
            if (!delete.deleteAll)
                query.and(GroupExpr.create(Collections.singletonMap("key",
                                           delete.key.getExpr(importExprs, session.modifier)),
                                           Where.TRUE,
                                           query.mapKeys).getWhere().not());

            Map<P, KeyExpr> intraKeyExprs = delete.deleteProperty.property.getMapKeys(); // генерим ключи (использовать будем только те, что не в DataObject
            Map<P, Expr> deleteExprs = new HashMap<P, Expr>();
            KeyExpr groupExpr = null;
            for (Map.Entry<P, ImportDeleteInterface> entry : ((PropertyImplement<P, ImportDeleteInterface>)delete.deleteProperty).mapping.entrySet()) {
                P propInt = entry.getKey();
                KeyExpr intraKeyExpr = intraKeyExprs.get(propInt);
                if (delete.key.equals(entry.getValue())) {
                    groupExpr = intraKeyExpr; // собственно группируем по этому ключу
                    deleteExprs.put(propInt, groupExpr);
                } else
                    deleteExprs.put(propInt, entry.getValue().getDeleteExpr(importTable, intraKeyExpr, session.modifier));
            }

            query.and(GroupExpr.create(Collections.singletonMap("key", groupExpr),
                                       delete.deleteProperty.property.getExpr(deleteExprs, session.modifier),
                                       GroupType.ANY,
                                       Collections.singletonMap("key", keyExpr)).getWhere());

            for (Iterator<Map<String, DataObject>> iterator = query.executeClasses(session).keySet().iterator(); iterator.hasNext();) {
                Map<String, DataObject> row = iterator.next();
                session.changeClass(row.get("key"), null, !iterator.hasNext());
            }
        }
    }

/*    private void deleteObjects(Map<ImportKey, List<DataObject>> keyValueLists) throws SQLException {
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
