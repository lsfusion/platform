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
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.cases.CaseJoin;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class SessionDataTable implements SessionData<SessionDataTable> {
    private SessionTable table;

    private List<KeyField> keys; // чисто для порядка ключей

    private Map<KeyField, DataObject> keyValues;
    private Map<PropertyField, ObjectValue> propertyValues;

    public SessionDataTable(SessionTable table, List<KeyField> keys, Map<KeyField, DataObject> keyValues, Map<PropertyField, ObjectValue> propertyValues) {
        assert keys.containsAll(table.keys);

        this.table = table;

        this.keys = keys;
        this.keyValues = keyValues;

        this.propertyValues = propertyValues;
    }

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {

        final Join<PropertyField> tableJoin = table.join(filterKeys(joinImplement, table.keys));
        return new Join<PropertyField>() {
            @Override
            public Expr getExpr(PropertyField property) {
                ObjectValue propertyValue = propertyValues.get(property);
                if(propertyValue!=null)
                    return propertyValue.getExpr();
                else
                    return tableJoin.getExpr(property);
            }

            @Override
            public Where getWhere() {
                return tableJoin.getWhere();
            }

            @Override
            public Collection<PropertyField> getProperties() {
                return SessionDataTable.this.getProperties();
            }
        }.and(CompareWhere.compareValues(filterKeys(joinImplement, keyValues.keySet()), keyValues));
    }

    public List<KeyField> getKeys() {
        return keys;
    }

    public Set<PropertyField> getProperties() {
        return BaseUtils.mergeSet(table.getProperties(), propertyValues.keySet());
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        int hash = table.hashValues(hashValues);
        hash += 31 * (MapValuesIterable.hash(keyValues, hashValues) ^ MapValuesIterable.hash(propertyValues, hashValues));
        return hash;
    }

    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        MapValuesIterable.enumValues(result, keyValues);
        MapValuesIterable.enumValues(result, propertyValues);
        result.add(table);
        return result;
    }

    public SessionDataTable translate(MapValuesTranslate mapValues) {
        return new SessionDataTable(table.translate(mapValues), keys,
                mapValues.translateValues(keyValues), mapValues.translateValues(propertyValues));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof SessionDataTable && keys.equals(((SessionDataTable) obj).keys) && table.equals(((SessionDataTable) obj).table) && keyValues.equals(((SessionDataTable) obj).keyValues);
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

    public SessionDataTable insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException {
        
        Map<KeyField, DataObject> fixedKeyValues = BaseUtils.mergeEquals(keyFields, keyValues);
        Map<PropertyField, ObjectValue> fixedPropValues = BaseUtils.mergeEquals(propFields, propertyValues);

        return new SessionDataTable(table.addFields(session, BaseUtils.filterNotList(keys, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(keyValues, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(propertyValues, fixedPropValues.keySet()), owner).
              insertRecord(session, BaseUtils.filterNotKeys(keyFields, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(propFields, fixedPropValues.keySet()), update, groupLast, owner),
                keys, fixedKeyValues, fixedPropValues);
    }

    // для оптимизации групповых добавлений (batch processing'а)
    public SessionDataTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, boolean groupLast, Object owner) throws SQLException {

        this.keys = keys;
        // сначала пробежим по всем проверим с какими field'ами создавать таблицы и заодно propertyClasses узнаем, после этого batch'ем запишем
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> it = rows.entrySet().iterator();
        Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> first = it.next();
        keyValues = new HashMap<KeyField, DataObject>(first.getKey());
        propertyValues = new HashMap<PropertyField, ObjectValue>(first.getValue());

        while(it.hasNext()) {
            Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> entry = it.next();
            BaseUtils.removeNotEquals(keyValues, entry.getKey());
            BaseUtils.removeNotEquals(propertyValues, entry.getValue());
        }

        // пробежим по всем вырежем equals и создадим classes
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> tableRows = new HashMap<Map<KeyField,DataObject>, Map<PropertyField,ObjectValue>>();
        for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet())
            tableRows.put(BaseUtils.filterNotKeys(row.getKey(), keyValues.keySet()), BaseUtils.filterNotKeys(row.getValue(), propertyValues.keySet()));
        table = SessionTable.create(session, BaseUtils.filterNotList(keys, keyValues.keySet()), BaseUtils.removeSet(properties, propertyValues.keySet()), tableRows, groupLast, owner);
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
        if(BaseUtils.filterKeys(deleteKeys, keyValues.keySet()).equals(keyValues)) //если константная часть ключа не равна, то нечего удалять
            table.deleteRecords(session, filterKeys(deleteKeys, table.keys));
        return this;
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        DataObject keyValue = keyValues.get(mapField);
        if (keyValue!=null) {
            if (keyValue.equals(object)) //удаляем всё
                return new SessionRows(keys, getProperties());
        } else
            table.deleteKey(session, mapField, object);
        return this;
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        ObjectValue propValue = propertyValues.get(property);
        if (propValue!=null) {
            if (propValue.equals(object)) //удаляем всё
                return new SessionRows(keys, getProperties());
        } else
            table.deleteProperty(session, property, object);
        return this;
    }

    private HashComponents<Value> components = null;

    @ManualLazy
    public HashComponents<Value> getComponents() {
        if (components == null) {
            components = AbstractMapValues.getComponents(this);
        }
        return components;
    }

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Key Values : " + keyValues);
        System.out.println("Prop Values : " + propertyValues);
        table.out(session);
    }
}
