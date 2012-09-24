package platform.server.session;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.BaseClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.RemapJoin;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class SessionTableUsage<K,V> implements MapKeysInterface<K> {

    protected SessionData<?> table;
    protected Map<KeyField, K> mapKeys;
    protected Map<PropertyField, V> mapProps;
    
    public Collection<K> getKeys() {
        return mapKeys.values();
    }

    public Collection<V> getValues() {
        return mapProps.values();
    }

    public Map<K, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(mapKeys.values());
    }

    public SessionTableUsage(List<K> keys, List<V> properties, Type.Getter<K> keyType, Type.Getter<V> propertyType) {
        List<KeyField> keyList = new ArrayList<KeyField>();
        mapKeys = new HashMap<KeyField, K>();
        for(K key : keys) {
            KeyField keyField = new KeyField("k"+mapKeys.size(),keyType.getType(key));
            keyList.add(keyField);
            mapKeys.put(keyField, key);
        }

        mapProps = new HashMap<PropertyField, V>();
        for(V property : properties) // нужен детерминированный порядок, хотя бы для StructChanges
            mapProps.put(new PropertyField("p"+mapProps.size(), propertyType.getType(property)), property);

        table = new SessionRows(keyList, mapProps.keySet());
    }


    public SessionTableUsage(SQLSession sql, final Query<K,V> query, BaseClass baseClass, QueryEnvironment env,
                             final Map<K, Type> keyTypes, final Map<V, Type> propertyTypes) throws SQLException { // здесь порядок особо не важен, так как assert что getUsage'а не будет
        this(new ArrayList<K>(query.mapKeys.keySet()), new ArrayList<V>(query.properties.keySet()), new Type.Getter<K>() {
            public Type getType(K key) {
                return keyTypes.get(key);
            }
        }, new Type.Getter<V>() {
            public Type getType(V key) {
                return propertyTypes.get(key);
            }
        });
        writeRows(sql, query, baseClass, env);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return new RemapJoin<V, PropertyField>(table.join(BaseUtils.join(mapKeys, joinImplement)), BaseUtils.reverse(mapProps));
    }

    public Where getWhere(Map<K, ? extends Expr> mapExprs) {
        return table.join(BaseUtils.join(mapKeys, mapExprs)).getWhere();
    }

    public void modifyRecord(SQLSession session, Map<K, DataObject> keyObjects, Map<V, ObjectValue> propertyObjects, Modify type) throws SQLException {
        table = table.modifyRecord(session, BaseUtils.join(mapKeys, keyObjects), BaseUtils.join(mapProps, propertyObjects), type, this);
    }

    public void writeKeys(SQLSession session,Collection<Map<K,DataObject>> writeRows) throws SQLException {
        writeRows(session, BaseUtils.<Map<K,DataObject>, Map<V, ObjectValue>>toMap(writeRows, new HashMap<V, ObjectValue>()));
    }

    public void writeRows(SQLSession session,Map<Map<K,DataObject>,Map<V,ObjectValue>> writeRows) throws SQLException {
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> mapWriteRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for(Map.Entry<Map<K, DataObject>, Map<V, ObjectValue>> writeRow : writeRows.entrySet())
            mapWriteRows.put(BaseUtils.join(mapKeys, writeRow.getKey()), BaseUtils.join(mapProps, writeRow.getValue()));
        table = table.rewrite(session, mapWriteRows, this);
    }

    public void writeRows(SQLSession session, Query<K, V> query, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        table = table.rewrite(session, new Query<KeyField,PropertyField>(query, mapKeys, mapProps), baseClass, env, this);
    }

    // добавляет ряды которых не было в таблице, или modify'ит
    public void modifyRows(SQLSession session, Query<K, V> query, BaseClass baseClass, Modify type, QueryEnvironment env) throws SQLException {
        table = table.modifyRows(session, new Query<KeyField, PropertyField>(query, mapKeys, type == Modify.DELETE ? new HashMap<PropertyField, V>() : mapProps), baseClass, type, env, this);
    }
    // оптимизационная штука
    public void updateAdded(SQLSession session, BaseClass baseClass, V property, int count) throws SQLException {
        table = table.updateAdded(session, baseClass, getField(property), count);
    }

    private PropertyField getField(V property) {
        return BaseUtils.reverse(mapProps).get(property);
    }

    /*    public void deleteProperty(SQLSession session, V property, DataObject object) throws SQLException {
        table = table.deleteProperty(session, getField(property), object);
    }*/

    public void drop(SQLSession session) throws SQLException {
        table.drop(session, this);
        table = null;
    }

    public Collection<Map<V, Object>> read(DataSession session, Map<K, DataObject> mapValues) throws SQLException {
        return read(mapValues, session.sql, session.env, new OrderedMap<V, Boolean>()).values();
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> read(DataSession session) throws SQLException {
        return read(session.sql, session.env, new OrderedMap<V, Boolean>());
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> read(FormInstance formInstance, OrderedMap<V, Boolean> orders) throws SQLException {
        return read(formInstance.session.sql, formInstance.getQueryEnv(), orders);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> read(SQLSession session, QueryEnvironment env, OrderedMap<V, Boolean> orders) throws SQLException {
        return read(new HashMap<K, DataObject>(), session, env, orders);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> read(Map<K, DataObject> mapValues, SQLSession session, QueryEnvironment env, OrderedMap<V, Boolean> orders) throws SQLException {
        Query<K, V> query = new Query<K,V>(mapKeys.values(), mapValues);
        Join<V> tableJoin = join(query.getMapExprs());
        query.properties.putAll(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.execute(session, orders, 0, env);
    }

    public <B> ClassWhere<B> getClassWhere(V property, Map<K, ? extends B> remapKeys, B mapProp) {
        return new ClassWhere<B>(table.getClassWhere(getField(property)),
                BaseUtils.merge(BaseUtils.join(mapKeys, remapKeys),
                                BaseUtils.rightJoin(mapProps, Collections.singletonMap(property, mapProp))));
    }

    public <B> ClassWhere<B> getClassWhere(Map<K, B> remapKeys) {
        return new ClassWhere<B>(table.getClassWhere(), BaseUtils.join(mapKeys, remapKeys));
    }


    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    public SessionData saveData() {
        return table;
    }
    public void rollData(SQLSession sql, SessionData table) throws SQLException {
        assert this.table == null;
        this.table = table;
        this.table.rollDrop(sql, this);
    }
    
    public static <T> Map<T, SessionData> saveData(Map<T, ? extends SessionTableUsage> map) {
        Map<T, SessionData> result = new HashMap<T, SessionData>();
        for(Map.Entry<T, ? extends SessionTableUsage> entry : map.entrySet())
            result.put(entry.getKey(), entry.getValue().saveData());
        return result;
    }

    // assert что не удаляется
    public static <T, D extends SessionTableUsage> Map<T, D> rollData(SQLSession sql, Map<T, D> map, Map<T, SessionData> rollback) throws SQLException {
        Map<T, D> result = new HashMap<T, D>();
        for(Map.Entry<T, SessionData> entry : rollback.entrySet()) {
            D table = map.get(entry.getKey());
            table.rollData(sql, entry.getValue());
            result.put(entry.getKey(), table);
        }
        return result;
    }

    public int getCount() {
        return table.getCount();
    }
}
