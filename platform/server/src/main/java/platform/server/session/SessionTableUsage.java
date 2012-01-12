package platform.server.session;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.ValuesContext;
import platform.server.classes.BaseClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.MapJoin;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class SessionTableUsage<K,V> implements MapKeysInterface<K> {

    protected SessionData<?> table;
    protected Map<KeyField, K> mapKeys;
    protected Map<PropertyField, V> mapProps;

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
        for(V property : properties) // нужен детерминированный порядок, хотя бы для UsedChanges
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
        return new MapJoin<V, PropertyField>(table.join(BaseUtils.join(mapKeys, joinImplement)), BaseUtils.reverse(mapProps));
    }

    public Where getWhere(Map<K, ? extends Expr> mapExprs) {
        return table.join(BaseUtils.join(mapKeys, mapExprs)).getWhere();
    }

    public void insertRecord(SQLSession session, Map<K, DataObject> keyObjects, Map<V, ObjectValue> propertyObjects, boolean update, boolean groupLast) throws SQLException {
        table = table.insertRecord(session, BaseUtils.join(mapKeys, keyObjects), BaseUtils.join(mapProps, propertyObjects), update, groupLast, this);
    }

    public void deleteRecords(SQLSession session, Map<K, DataObject> keyObjects) throws SQLException {
        table = table.deleteRecords(session, BaseUtils.join(mapKeys, keyObjects));
    }

    public void writeKeys(SQLSession session,Collection<Map<K,DataObject>> writeRows) throws SQLException {
        table = table.rewrite(session, BaseUtils.joinCol(mapKeys, writeRows), this);
    }

    public void writeRows(SQLSession session, Query<K, V> query, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        table = table.rewrite(session, new Query<KeyField,PropertyField>(query, mapKeys, mapProps), baseClass, env, this);
    }

    public void addRows(SQLSession session, Query<K, V> query, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        Query<K, V> addQuery = new Query<K, V>(query.mapKeys);
        Join<V> prevJoin = join(addQuery.mapKeys);

        Where prevWhere = prevJoin.getWhere();
        addQuery.and(prevWhere.or(query.where));
        for(Map.Entry<V, Expr> property : query.properties.entrySet())
            addQuery.properties.put(property.getKey(), prevJoin.getExpr(property.getKey()).ifElse(prevWhere, property.getValue()));
        writeRows(session, addQuery, baseClass, env);
    }

    private PropertyField getField(V property) {
        return BaseUtils.reverse(mapProps).get(property);
    }

    public void deleteKey(SQLSession session, K key, DataObject object) throws SQLException {
        table = table.deleteKey(session, BaseUtils.reverse(mapKeys).get(key), object);
    }

    public void deleteProperty(SQLSession session, V property, DataObject object) throws SQLException {
        table = table.deleteProperty(session, getField(property), object);
    }

    public void drop(SQLSession session) throws SQLException {
        table.drop(session, this);
        table = null;
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> read(SQLSession session, QueryEnvironment env, OrderedMap<V, Boolean> orders) throws SQLException {
        Query<K, V> query = new Query<K,V>(mapKeys.values());
        Join<V> tableJoin = join(query.mapKeys);
        query.properties.putAll(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.execute(session, orders, 0, env);
    }

    // assert что ни вызывается при нижнем конструкторе пока
    public ValuesContext getUsage() { // IMMUTABLE
        return table;
    }

    public SessionTableUsage(SessionData<?> table, Map<KeyField, K> mapKeys, Map<PropertyField, V> mapProps) {
        this.table = table;
        this.mapKeys = mapKeys;
        this.mapProps = mapProps;
    }

    public <MK> SessionTableUsage<MK, V> map(Map<K, MK> remapKeys) {
        return new SessionTableUsage<MK, V>(table, BaseUtils.join(mapKeys, remapKeys), mapProps);
    }

    public <MK, MV> SessionTableUsage<MK, MV> map(Map<K, MK> remapKeys, Map<V, MV> remapProps) {
        return new SessionTableUsage<MK, MV>(table, BaseUtils.join(mapKeys, remapKeys), BaseUtils.join(mapProps, remapProps));
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
}
