package platform.server.data;

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

public class SessionFixedFieldsTable implements SessionData<SessionFixedFieldsTable> {
    private SessionTable table;

    private List<KeyField> keys;
    private List<KeyField> constantKeys;
    private Map<KeyField, DataObject> constantKeyValues;

    private Set<PropertyField> properties;
    private Set<PropertyField> constantProperties;
    private Map<PropertyField, ObjectValue> constantPropertyValues;

    public SessionFixedFieldsTable(SessionTable table, List<KeyField> keys, Set<PropertyField> properties, Map<KeyField, DataObject> constantKeyValues, Map<PropertyField, ObjectValue> constantPropertyValues) {
        assert keys.containsAll(table.keys);

        this.table = table;

        this.keys = keys;
        this.constantKeys = removeList(keys, table.keys);
        this.constantKeyValues = constantKeyValues;

        this.properties = properties;
        this.constantProperties = removeSet(properties, table.properties);
        this.constantPropertyValues = constantPropertyValues;
    }

    public SessionFixedFieldsTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Map<KeyField, DataObject> constantKeyValues, Map<PropertyField, ObjectValue> constantPropertyValues, Object owner) throws SQLException {
        this.keys = keys;
        this.constantKeys = filterList(keys, constantKeyValues.keySet());
        this.constantKeyValues = constantKeyValues;

        this.properties = properties;
        this.constantProperties = filterSet(properties, constantPropertyValues.keySet());
        this.constantPropertyValues = constantPropertyValues;

        List<KeyField> tableKeys = filterNotList(keys, constantKeys);
        Set<PropertyField> tableProperties = filterNotSet(properties, constantProperties);
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> tableRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for (Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> entry : rows.entrySet()) {
            tableRows.put(filterNotKeys(entry.getKey(), constantKeys), filterNotKeys(entry.getValue(), constantProperties));
        }

        this.table = new SessionTable(session, tableKeys, tableProperties, tableRows, owner);
    }

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {
        final Map<KeyField, ? extends Expr> tableJoinImplement = filterKeys(joinImplement, table.keys);
        final Map<KeyField, ? extends Expr> rowsJoinImplement = filterKeys(joinImplement, constantKeys);

        final Join<PropertyField> tableJoin = table.join(tableJoinImplement);

        final Where constantKeysWhere = CompareWhere.compareValues(rowsJoinImplement, constantKeyValues);

//        return new CaseJoin<PropertyField>(constantKeysWhere, tableJoin);
        return new Join<PropertyField>() {
            public Expr getExpr(PropertyField property) {
                ExprCaseList result = constantProperties.contains(property)
                                      ? new ExprCaseList(constantKeysWhere, constantPropertyValues.get(property).getExpr())
                                      : new ExprCaseList(constantKeysWhere, tableJoin.getExpr(property));
                return result.getExpr();
            }

            public Where getWhere() {
                return constantKeysWhere.and(tableJoin.getWhere());
            }

            public Collection<PropertyField> getProperties() {
                return properties;
            }
        };
    }

    public List<KeyField> getKeys() {
        return keys;
    }

    public Set<PropertyField> getProperties() {
        return properties;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        int hash = table.hashValues(hashValues);
        hash += 31 * (MapValuesIterable.hash(constantKeyValues, hashValues) ^ MapValuesIterable.hash(constantPropertyValues, hashValues));
        return hash;
    }

    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        MapValuesIterable.enumValues(result, constantKeyValues);
        MapValuesIterable.enumValues(result, constantPropertyValues);
        result.add(table);
        return result;
    }

    public SessionFixedFieldsTable translate(MapValuesTranslate mapValues) {
        Map<KeyField, DataObject> translatedKeyValues = mapValues.translateValues(constantKeyValues);
        Map<PropertyField, ObjectValue> translatedPropertyValues = mapValues.translateValues(constantPropertyValues);
        SessionTable newTable = table.translate(mapValues);
        return new SessionFixedFieldsTable(newTable, keys, properties, translatedKeyValues, translatedPropertyValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof SessionFixedFieldsTable)) {
            return false;
        }

        SessionFixedFieldsTable other = (SessionFixedFieldsTable) obj;
        return keys.equals(other.keys) && table.equals(other.table) && constantKeyValues.equals(other.constantKeyValues);
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
        Map<PropertyField, ObjectValue> constantPropertiesFields = filterKeys(propFields, constantProperties);

        boolean needToMigrate = false;

        for (Map.Entry<KeyField, DataObject> entry : constantKeysFields.entrySet()) {
            if (!entry.getValue().equals(constantKeyValues.get(entry.getKey()))) {
                //постоянные ключи не совпадают, поэтому мигрируем на таблицу...
                needToMigrate = true;
                break;
            }
        }

        if (!needToMigrate) {
            for (Map.Entry<PropertyField, ObjectValue> entry : constantPropertiesFields.entrySet()) {
                if (!entry.getValue().equals(constantPropertyValues.get(entry.getKey()))) {
                    //постоянные ключи не совпадают, поэтому мигрируем на таблицу...
                    needToMigrate = true;
                    break;
                }
            }
        }

        if (needToMigrate) {
            //...вставляем запись и возвращаем новую таблицу
            return migrateToSessionTable(session, owner).insertRecord(session, keyFields, propFields, update, owner);
        }


        return copyIfTableChanged(table.insertRecord(session, filterKeys(keyFields, table.keys), filterKeys(propFields, table.properties), update, owner));
    }

    private SessionData copyIfTableChanged(SessionTable newTable) {
        return newTable == table
               ? this
               : new SessionFixedFieldsTable(newTable, keys, properties, constantKeyValues, constantPropertyValues);
    }

    private SessionTable migrateToSessionTable(SQLSession session, Object owner) throws SQLException {
        Map<KeyField, KeyExpr> mapKeys = KeyExpr.getMapKeys(keys);

        Join<PropertyField> tableJoin = table.join(filterKeys(mapKeys, table.keys));

        Map<PropertyField, Expr> mapProperties = new HashMap(tableJoin.getExprs());
        for (PropertyField propertyField : constantProperties) {
            mapProperties.put(propertyField, constantPropertyValues.get(propertyField).getExpr());
        }

        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(mapKeys, mapProperties);
        query.and(CompareWhere.compareValues(filterKeys(query.mapKeys, constantKeys), constantKeyValues));
        query.and(tableJoin.getWhere());

        // читаем классы не считывая данные
        ClassWhere<KeyField> classes = query.getClassWhere(properties);
        Map<PropertyField, ClassWhere<Field>> propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for (PropertyField field : query.properties.keySet()) {
            propertyClasses.put(field, query.<Field>getClassWhere(Collections.singleton(field)));
        }

        SessionTable newTable = new SessionTable(session, keys, properties, classes, propertyClasses, owner);

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
        Map<KeyField, DataObject> constantDeleteKeyValues = filterKeys(deleteKeys, constantKeys);
        for (Map.Entry<KeyField, DataObject> entry : constantDeleteKeyValues.entrySet()) {
            //если константная часть ключа не равна, то нечего удалять
            if (!entry.getValue().equals(constantKeyValues.get(entry.getKey()))) {
                return this;
            }
        }

        return copyIfTableChanged(table.deleteRecords(session, filterKeys(deleteKeys, table.keys)));
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        SessionTable newTable = table;
        if (constantKeyValues.containsKey(mapField)) {
            if (constantKeyValues.get(mapField).equals(object)) {
                //удаляем всё
                newTable = table.deleteAllRecords(session);
            }
        } else {
            newTable = table.deleteKey(session, mapField, object);
        }

        return copyIfTableChanged(newTable);
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        SessionTable newTable = table;
        if (constantPropertyValues.containsKey(property)) {
            if (constantPropertyValues.get(property).equals(object)) {
                //удаляем всё
                newTable = table.deleteAllRecords(session);
            }
        } else {
            newTable = table.deleteProperty(session, property, object);
        }

        return copyIfTableChanged(newTable);
    }

    private HashComponents<Value> components = null;

    @ManualLazy
    public HashComponents<Value> getComponents() {
        if (components == null) {
            components = AbstractMapValues.getComponents(this);
        }
        return components;
    }
}
