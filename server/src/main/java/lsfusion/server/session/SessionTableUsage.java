package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.InnerContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Map;

public class SessionTableUsage<K,V> implements MapKeysInterface<K>, TableOwner {

    protected SessionData<?> table;
    protected ImRevMap<KeyField, K> mapKeys;
    protected ImRevMap<PropertyField, V> mapProps;

    public boolean used(InnerContext context) {
        return table.used(context);
    }

    public ImSet<K> getKeys() {
        return mapKeys.valuesSet();
    }

    public ImSet<V> getValues() {
        return mapProps.valuesSet();
    }

    public ImRevMap<K, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(getKeys());
    }

    public static <K, V, P extends PropertyInterface> PropertyChange<P> getChange(SessionTableUsage<K, V> table, ImRevMap<P, K> map, V value) {
        ImRevMap<K, KeyExpr> mapKeys = table.getMapKeys();
        Join<V> join = table.join(mapKeys);
        return new PropertyChange<P>(map.join(mapKeys), join.getExpr(value), join.getWhere());
    }

    public SessionTableUsage(ImOrderSet<K> keys, ImOrderSet<V> properties, final Type.Getter<K> keyType, final Type.Getter<V> propertyType) {
        ImRevMap<K, KeyField> revMapKeys = keys.mapOrderRevValues(new GetIndexValue<KeyField, K>() {
            public KeyField getMapValue(int i, K value) {
                return new KeyField("k" + i, keyType.getType(value));
            }});
        mapKeys = revMapKeys.reverse();

        mapProps = properties.mapOrderRevKeys(new GetIndexValue<PropertyField, V>() { // нужен детерминированный порядок, хотя бы для StructChanges
            public PropertyField getMapValue(int i, V value) {
                return new PropertyField("p" + i, propertyType.getType(value));
            }});

        table = new SessionRows(keys.mapOrder(revMapKeys), mapProps.keys());
    }


    public SessionTableUsage(SQLSession sql, final Query<K,V> query, BaseClass baseClass, QueryEnvironment env,
                             final ImMap<K, Type> keyTypes, final ImMap<V, Type> propertyTypes) throws SQLException, SQLHandledException { // здесь порядок особо не важен, так как assert что getUsage'а не будет
        this(query.mapKeys.keys().toOrderSet(), query.properties.keys().toOrderSet(), new Type.Getter<K>() {
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

    public Join<V> join(ImMap<K, ? extends Expr> joinImplement) {
        return new RemapJoin<V, PropertyField>(table.join(mapKeys.join(joinImplement)), mapProps.reverse());
    }

    public Where getWhere(ImMap<K, ? extends Expr> mapExprs) {
        return table.join(mapKeys.join(mapExprs)).getWhere();
    }

    private ModifyResult aspectModify(SessionData<?> newTable, Boolean dataChanged) {
        boolean sourceChanged = !BaseUtils.hashEquals(table, newTable);
        table = newTable;
        if(sourceChanged) // теоретический при этом может быть не dataChanged (что часто баг но не всегда, например при добавлении полей)
            return ModifyResult.DATA_SOURCE;
        if(dataChanged != null && dataChanged)
            return ModifyResult.DATA;
        return ModifyResult.NO;
    }

    public ModifyResult modifyRecord(SQLSession session, ImMap<K, DataObject> keyObjects, ImMap<V, ObjectValue> propertyObjects, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        Result<Boolean> changed = new Result<Boolean>();
        return aspectModify(table.modifyRecord(session, mapKeys.join(keyObjects), mapProps.join(propertyObjects), type, this, owner, changed), changed.result);
    }

    public void writeKeys(SQLSession session,ImSet<ImMap<K,DataObject>> writeRows, OperationOwner owner) throws SQLException, SQLHandledException {
        writeRows(session, writeRows.toMap(MapFact.<V, ObjectValue>EMPTY()), owner);
    }

    public void writeRows(SQLSession session,ImMap<ImMap<K,DataObject>,ImMap<V,ObjectValue>> writeRows, OperationOwner opOwner) throws SQLException, SQLHandledException {
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> mapWriteRows = writeRows.mapKeyValues(new GetValue<ImMap<KeyField, DataObject>, ImMap<K, DataObject>>() {
            public ImMap<KeyField, DataObject> getMapValue(ImMap<K, DataObject> value) {
                return mapKeys.join(value);
            }}, new GetValue<ImMap<PropertyField, ObjectValue>, ImMap<V, ObjectValue>>() {
            public ImMap<PropertyField, ObjectValue> getMapValue(ImMap<V, ObjectValue> value) {
                return mapProps.join(value);
            }});
        table = table.rewrite(session, mapWriteRows, this, opOwner);
    }

    public void writeRows(SQLSession session, IQuery<K, V> query, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        table = table.rewrite(session, query.map(mapKeys, mapProps), baseClass, env, this);
    }

    // добавляет ряды которых не было в таблице, или modify'ит
    public ModifyResult modifyRows(SQLSession session, IQuery<K, V> query, BaseClass baseClass, Modify type, QueryEnvironment env) throws SQLException, SQLHandledException {
        Result<Boolean> changed = new Result<Boolean>();
        return aspectModify(table.modifyRows(session, query.map(mapKeys, type == Modify.DELETE ? MapFact.<PropertyField, V>EMPTYREV() : mapProps), baseClass, type, env, this, changed), changed.result);
    }
    // оптимизационная штука
    public void updateAdded(SQLSession session, BaseClass baseClass, V property, Pair<Integer,Integer>[] shifts, OperationOwner owner) throws SQLException, SQLHandledException {
        table = table.updateAdded(session, baseClass, getField(property), shifts, owner, this);
    }

    private PropertyField getField(V property) {
        return mapProps.reverse().get(property);
    }

    /*    public void deleteProperty(SQLSession session, V property, DataObject object) throws SQLException {
        table = table.deleteProperty(session, getField(property), object);
    }*/

    public void drop(SQLSession session, OperationOwner owner) throws SQLException {
        table.drop(session, this, owner);
        table = null;
    }

    public ImCol<ImMap<V, Object>> read(DataSession session, ImMap<K, DataObject> mapValues) throws SQLException, SQLHandledException {
        return read(mapValues, session.sql, session.env, MapFact.<V, Boolean>EMPTYORDER()).values();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(DataSession session) throws SQLException, SQLHandledException {
        return read(session.sql, session.env, MapFact.<V, Boolean>EMPTYORDER());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(FormInstance formInstance, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return read(formInstance.session.sql, formInstance.getQueryEnv(), orders);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(SQLSession session, QueryEnvironment env, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return read(MapFact.<K, DataObject>EMPTY(), session, env, orders);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(ImMap<K, DataObject> mapValues, SQLSession session, QueryEnvironment env, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        QueryBuilder<K, V> query = new QueryBuilder<K,V>(mapKeys.valuesSet(), mapValues);
        Join<V> tableJoin = join(query.getMapExprs());
        query.addProperties(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.execute(session, orders, 0, env);
    }

    public ImSet<Object> readDistinct(V prop, SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        ImRevMap<K, KeyExpr> mapKeys = getMapKeys();
        KeyExpr key = new KeyExpr("key");
        Expr groupExpr = GroupExpr.create(MapFact.singleton("key", join(mapKeys).getExpr(prop)), ValueExpr.TRUE, GroupType.ANY, MapFact.<String, Expr>singleton("key", key));
        return new Query<String, Object>(MapFact.singletonRev("key", key), groupExpr.getWhere()).execute(session, owner).keyOrderSet().getSet().mapSetValues(new GetValue<Object, ImMap<String, Object>>() {
            public Object getMapValue(ImMap<String, Object> value) {
                return value.singleValue();
            }});
    }

    public <B> ClassWhere<B> getClassWhere(V property, ImRevMap<K, ? extends B> remapKeys, B mapProp) {
        ClassWhere<Field> classWhere = table.getClassWhere(getField(property));
        return new ClassWhere<B>(classWhere,
                MapFact.addRevExcl(mapKeys.join(remapKeys),
                        mapProps.rightJoin(MapFact.singletonRev(property, mapProp))));
    }

    public <B> ClassWhere<B> getClassWhere(ImRevMap<K, B> remapKeys) {
        return new ClassWhere<B>(table.getClassWhere(), mapKeys.join(remapKeys));
    }


    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    public SessionData saveData() {
        return table;
    }
    public void rollData(SQLSession sql, SessionData table, OperationOwner owner) throws SQLException {
        assert this.table == null;
        this.table = table;
        this.table.rollDrop(sql, this, owner);
    }
    
    public static <T> ImMap<T, SessionData> saveData(Map<T, ? extends SessionTableUsage> map) {
        return MapFact.<T, SessionTableUsage>fromJavaMap(map).mapValues(new GetValue<SessionData, SessionTableUsage>() {
            public SessionData getMapValue(SessionTableUsage value) {
                return value.saveData();
            }});
    }

    public int getCount() {
        return table.getCount();
    }

    @Override
    public String toString() {
        return "SU@" + System.identityHashCode(this) + " " + table.toString();
    }

    public void updateCurrentClasses(DataSession session) throws SQLException, SQLHandledException {
        table = table.updateCurrentClasses(session);
    }
}
