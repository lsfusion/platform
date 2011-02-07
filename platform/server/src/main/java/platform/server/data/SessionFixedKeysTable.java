package platform.server.data;

import platform.base.BaseUtils;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class SessionFixedKeysTable implements SessionData<SessionFixedKeysTable> {
    private List<KeyField> keys;

    private SessionTable table;
    private List<KeyField> constantKeys;
    private Map<KeyField, DataObject> constantRowKey;

    public SessionFixedKeysTable(SessionTable table, Map<KeyField, DataObject> constantRowKey, List<KeyField> keys) {
        assert keys.containsAll(table.keys);

        this.keys = keys;
        this.table = table;
        this.constantKeys = removeList(keys, table.keys);
        this.constantRowKey = constantRowKey;
    }

    public SessionFixedKeysTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Map<KeyField, DataObject> constantRowKey, Object owner) throws SQLException {
        this.keys = keys;
        this.constantKeys = filterList(keys, constantRowKey.keySet());
        this.constantRowKey = constantRowKey;

        List<KeyField> tableKeys = filterNotList(keys, constantKeys);
        ClassWhere<KeyField> tableClasses = new ClassWhere<KeyField>();
        for (Map<KeyField, DataObject> row : rows.keySet()) {
            tableClasses = tableClasses.or(new ClassWhere<KeyField>(DataObject.getMapClasses(filterKeys(row, tableKeys))));
        }

        for (Map<KeyField, DataObject> key : rows.keySet()) {
            for (KeyField constantKey : constantKeys) {
                key.remove(constantKey);
            }
        }

        this.table = new SessionTable(session, tableKeys, properties, tableClasses, propertyClasses, rows, owner);
    }

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {
        final Map<KeyField, ? extends Expr> tableJoinImplement = filterKeys(joinImplement, table.keys);
        final Map<KeyField, ? extends Expr> rowsJoinImplement = filterKeys(joinImplement, constantKeys);

        final Join<PropertyField> tableJoin = table.join(tableJoinImplement);

        final Where constantKeysWhere = CompareWhere.compareValues(rowsJoinImplement, constantRowKey);


        return new Join<PropertyField>() {
            public Expr getExpr(PropertyField property) {
                return new ExprCaseList(
                        constantKeysWhere, tableJoin.getExpr(property)
                ).getExpr();
            }

            public Where getWhere() {
                return constantKeysWhere.and(tableJoin.getWhere());
            }

            public Collection<PropertyField> getProperties() {
                return tableJoin.getProperties();
            }
        };
    }

    public List<KeyField> getKeys() {
        return keys;
    }

    public Set<PropertyField> getProperties() {
        return table.getProperties();
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        int hash = table.hashValues(hashValues);
        hash += 31 * MapValuesIterable.hash(constantRowKey, hashValues);
        return hash;
    }

    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        MapValuesIterable.enumValues(result, constantRowKey);
        result.add(table);
        return result;
    }

    public SessionFixedKeysTable translate(MapValuesTranslate mapValues) {
        Map<KeyField, DataObject> translatedRow = mapValues.translateValues(constantRowKey);
        SessionTable newTable = table.translate(mapValues);
        return new SessionFixedKeysTable(newTable, translatedRow, keys);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof SessionFixedKeysTable)) {
            return false;
        }

        SessionFixedKeysTable other = (SessionFixedKeysTable) obj;
        return keys.equals(other.keys) && table.equals(other.table) && constantRowKey.equals(other.constantRowKey);
    }

    boolean hashCoded = false;
    int hashCode;

    @Override
    public int hashCode() {
        if (!hashCoded) {
            hashCode = hashValues(HashCodeValues.instance);
            hashCoded = true;
        }
        return hashCode;
    }

    public SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, Object owner) throws SQLException {
        Map<KeyField, DataObject> constantKeysFields = filterKeys(keyFields, constantKeys);
        for (Map.Entry<KeyField, DataObject> entry : constantKeysFields.entrySet()) {
            if (!entry.getValue().equals(constantRowKey.get(entry.getKey()))) {
                //постоянные ключи не совпадают, поэтому мигрируем на таблицу...
                SessionTable migrateTable = migrateToSessionTable(session, owner);

                //...вставляем запись и возвращаем новую таблицу
                return migrateTable.insertRecord(session, keyFields, propFields, update, owner);
            }
        }

        return copyIfTableChanged(table.insertRecord(session, filterKeys(keyFields, table.keys), propFields, update, owner));
    }

    private SessionData copyIfTableChanged(SessionTable newTable) {
        return newTable == table
               ? this
               : new SessionFixedKeysTable(newTable, constantRowKey, keys);
    }

    private SessionTable migrateToSessionTable(SQLSession session, Object owner) throws SQLException {
        Map<KeyField, KeyExpr> mapKeys = KeyExpr.getMapKeys(keys);
        Map<PropertyField, Expr> properties = table.join(filterKeys(mapKeys, table.keys)).getExprs();

        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(mapKeys, properties);
        query.and(CompareWhere.compareValues(filterKeys(query.mapKeys, constantKeys), constantRowKey));
        query.and(table.join(filterKeys(query.mapKeys, table.keys)).getWhere());

        // читаем классы не считывая данные
        ClassWhere<KeyField> classes = query.<KeyField>getClassWhere(table.properties);
        Map<PropertyField, ClassWhere<Field>> propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for (PropertyField field : query.properties.keySet()) {
            propertyClasses.put(field, query.<Field>getClassWhere(Collections.singleton(field)));
        }

        SessionTable newTable = new SessionTable(session, keys, table.properties, classes, propertyClasses, owner);

        session.insertSelect(new ModifyQuery(newTable, query));
        return newTable;
    }

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, writeRows, owner);
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        table.drop(session, owner);
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, query, baseClass, env, owner);
    }

    public SessionData deleteRecords(SQLSession session, Map<KeyField, DataObject> deleteKeys) throws SQLException {
        Map<KeyField, DataObject> constantKeysValues = filterKeys(deleteKeys, constantKeys);
        for (Map.Entry<KeyField, DataObject> entry : constantKeysValues.entrySet()) {
            //если константная часть ключа не равна, то нечего удалять
            if (!entry.getValue().equals(constantRowKey.get(entry.getKey()))) {
                return this;
            }
        }

        return copyIfTableChanged(table.deleteRecords(session, filterKeys(deleteKeys, table.keys)));
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        SessionTable newTable = table;
        if (constantRowKey.containsKey(mapField)) {
            if (constantRowKey.get(mapField).equals(object)) {
                //удаляем всё
                newTable = table.deleteAllRecords(session);
            }
        } else {
            newTable = table.deleteKey(session, mapField, object);
        }

        return copyIfTableChanged(newTable);
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        return copyIfTableChanged(table.deleteProperty(session, property, object));
    }

    private BaseUtils.HashComponents<Value> components = null;

    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if (components == null) {
            components = AbstractMapValues.getComponents(this);
        }
        return components;
    }
}
