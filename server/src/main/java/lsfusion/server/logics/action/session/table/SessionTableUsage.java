package lsfusion.server.logics.action.session.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.InnerContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.query.translate.RemapJoin;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.ModifyResult;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

import static lsfusion.server.data.table.SessionData.castTypes;

public class SessionTableUsage<K,V> implements MapKeysInterface<K>, TableOwner {

    protected SessionData<?> table;
    protected ImRevMap<KeyField, K> mapKeys;
    protected ImRevMap<PropertyField, V> mapProps; // должны учитывать correlatedExprs (либо aspectNoCorrelatedExprs)

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
        return new PropertyChange<>(map.join(mapKeys), join.getExpr(value), join.getWhere());
    }

    public static <K> ImRevMap<KeyField, K> genKeys(ImOrderSet<K> keys, final Type.Getter<K> keyType) {
        return keys.mapOrderRevKeys((i, value) -> new KeyField("k" + i, keyType.getType(value)));
    }

    public static <V> ImRevMap<PropertyField, V> genProps(ImOrderSet<V> properties, final Type.Getter<V> propertyType) {
        return genProps(0, properties, propertyType);
    }    
    public static <V> ImRevMap<PropertyField, V> genProps(final int offset, ImOrderSet<V> properties, final Type.Getter<V> propertyType) {
        // нужен детерминированный порядок, хотя бы для StructChanges
        return properties.mapOrderRevKeys((i, value) -> new PropertyField("p" + i + offset, propertyType.getType(value)));
    }

    //    public String stack;
    public String debugInfo;

    @Override
    public String getDebugInfo() {
        return debugInfo;
    }

    public SessionTableUsage(String debugInfo, ImOrderSet<K> keys, ImOrderSet<V> properties, final Type.Getter<K> keyType, final Type.Getter<V> propertyType) {
        mapKeys = genKeys(keys, keyType);
        mapProps = genProps(properties, propertyType);

        this.debugInfo = debugInfo;

        if(!postponeInitTable())
            initTable(keys);
//        stack = ExceptionUtils.getStackTrace();
    }

    // изврат конечно, но по другому непонятно как
    protected boolean postponeInitTable() {
        return false;
    }
    protected void initTable(ImOrderSet<K> keys) {
        table = new SessionRows(keys.mapOrder(mapKeys.reverse()), getFullProps());
    }

    protected ImSet<PropertyField> getFullProps() {
        return mapProps.keys();
    }

//    public String stack;

    public SessionTableUsage(String debugInfo, SQLSession sql, final Query<K, V> query, BaseClass baseClass, QueryEnvironment env,
                             final ImMap<K, Type> keyTypes, final ImMap<V, Type> propertyTypes, int selectTop) throws SQLException, SQLHandledException { // здесь порядок особо не важен, так как assert что getUsage'а не будет
        this(debugInfo, query.mapKeys.keys().toOrderSet(), query.properties.keys().toOrderSet(), keyTypes::get, propertyTypes::get);
        writeRows(sql, query, baseClass, env, SessionTable.matExprLocalQuery, selectTop);
    }

    public Join<V> join(ImMap<K, ? extends Expr> joinImplement) {
        ImMap<KeyField, ? extends Expr> mapExprs = mapKeys.join(joinImplement);
        return fullJoin(table.join(mapExprs), joinImplement);
    }
    
    protected Join<V> fullJoin(Join<PropertyField> join, ImMap<K, ? extends Expr> joinImplement) {
        return new RemapJoin<>(join, mapProps.reverse());
    }

    public Where getGroupWhere(ImMap<K, ? extends Expr> mapExprs) {
        if(mapKeys.size() == mapExprs.size()) // optimization
            return getWhere(mapExprs);
        
        ImRevMap<K, KeyExpr> mapKeys = getMapKeys();
        return GroupExpr.create(mapKeys.filterIncl(mapExprs.keys()), getWhere(mapKeys), mapExprs).getWhere();
    }

    public Where getWhere(ImMap<K, ? extends Expr> mapExprs) {
        return join(mapExprs).getWhere();
    }

    protected ModifyResult aspectModify(SessionData<?> newTable, Boolean dataChanged) {
        boolean sourceChanged = !BaseUtils.hashEquals(table, newTable);
        table = newTable;
        if(sourceChanged) // теоретический при этом может быть не dataChanged (что часто баг но не всегда, например при добавлении полей)
            return ModifyResult.DATA_SOURCE;
        if(dataChanged != null && dataChanged)
            return ModifyResult.DATA;
        return ModifyResult.NO;
    }

    // в общем случае надо гарантировать целостность ссылки table и usedTempTables в session, но так как отслеживать момент вернулась таблица или нет до exception'а, не хочется, пока такой мини-хак
    // есть еще несколько записей в table без этого аспекта, но там проблемы с ссылками быть не может (так как exception'ов нет)
    protected void aspectException(SQLSession session, OperationOwner owner) throws SQLException {
        table.rollDrop(session, this, owner, false);
    }
    
    public boolean hasCorrelations() {
        return false;
    }

    protected boolean aspectNoCorrelations() { // тут на самом деле в стеке нет добавления в запрос / ряды correlations, но предполагается что этот метод просто не вызывается из SessionTableUsage у которых есть correlatedExprs (см. использования hasCorrelatedExprs) 
        return !hasCorrelations();
    }

    public ModifyResult modifyRecord(SQLSession session, ImMap<K, DataObject> keyObjects, ImMap<V, ObjectValue> propertyObjects, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        assert aspectNoCorrelations();

        ImMap<KeyField, DataObject> keyFieldObjects = mapKeys.join(keyObjects);
        ImMap<PropertyField, ObjectValue> propFieldObjects = mapProps.join(propertyObjects);
        
        if(table instanceof SessionRows) {
            keyFieldObjects = castTypes(keyFieldObjects); // так как иначе можно unique violation получить
            propFieldObjects = castTypes(propFieldObjects); // иначе будет храниться значение другого типа
        }
        
        Result<Boolean> changed = new Result<>();
        try {
            return aspectModify(table.modifyRecord(session, keyFieldObjects, propFieldObjects, type, this, owner, changed), changed.result);
        } catch (Throwable t) {
            aspectException(session, owner);
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }

    public void writeKeys(SQLSession session,ImSet<ImMap<K,DataObject>> writeRows, OperationOwner owner) throws SQLException, SQLHandledException {
        writeRows(session, writeRows.toMap(MapFact.<V, ObjectValue>EMPTY()), owner);
    }

    public void writeRows(SQLSession session,ImMap<ImMap<K,DataObject>,ImMap<V,ObjectValue>> writeRows, OperationOwner opOwner) throws SQLException, SQLHandledException {
        assert aspectNoCorrelations();
        
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> mapWriteRows = writeRows.mapKeyValues(value -> mapKeys.join(value), value -> mapProps.join(value));
        // concerning castTypes some branches are safe (where field field type is guaranteed to be the same as in ObjectValue), however most branches are not
        // we need this casts not only for SessionRows, but also for SessionDataTable, because insertBatch with writeParam is used, and inside writeParam there are assertions that types are correct
        mapWriteRows = mapWriteRows.mapKeyValues(SessionData::castTypes, (Function<ImMap<PropertyField, ObjectValue>, ImMap<PropertyField, ObjectValue>>) SessionData::castTypes);
        
        try {
            table = table.rewrite(session, mapWriteRows, this, opOwner);
        } catch (Throwable t) {
            aspectException(session, opOwner);
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }

    public void writeRows(SQLSession session, IQuery<K, V> query, BaseClass baseClass, QueryEnvironment env, boolean updateClasses) throws SQLException, SQLHandledException {
        writeRows(session, query, baseClass, env, updateClasses, 0);
    }
    
    protected IQuery<KeyField, PropertyField> fullMap(IQuery<K, V> query) {
        return query.map(mapKeys, mapProps);
    }

    public void writeRows(SQLSession session, IQuery<K, V> query, BaseClass baseClass, QueryEnvironment env, boolean updateClasses, int selectTop) throws SQLException, SQLHandledException {
        try {
            table = table.rewrite(session, fullMap(query), baseClass, env, this, updateClasses, selectTop);
        } catch (Throwable t) {
            aspectException(session, env.getOpOwner());
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }

    // добавляет ряды которых не было в таблице, или modify'ит
    public ModifyResult modifyRows(SQLSession session, IQuery<K, V> query, BaseClass baseClass, Modify type, QueryEnvironment env, boolean updateClasses) throws SQLException, SQLHandledException {
        if(query.isEmpty()) // оптимизация
            return ModifyResult.NO;

        Result<Boolean> changed = new Result<>();
        try {
            return aspectModify(table.modifyRows(session, type == Modify.DELETE ? query.map(mapKeys,  MapFact.<PropertyField, V>EMPTYREV()) : fullMap(query), baseClass, type, env, this, changed, updateClasses), changed.result);
        } catch (Throwable t) {
            aspectException(session, env.getOpOwner());
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }
    // оптимизационная штука
    public void updateAdded(SQLSession session, BaseClass baseClass, V property, Pair<Long, Long>[] shifts, OperationOwner owner) throws SQLException, SQLHandledException {
        table = table.updateAdded(session, baseClass, getField(property), shifts, owner, this);
    }

    private PropertyField getField(V property) {
        return mapProps.reverse().get(property);
    }

    /*    public void deleteProperty(SQLSession session, V property, DataObject object) throws SQLException {
        table = table.deleteProperty(session, getField(property), object);
    }*/

    public void drop(SQLSession session, OperationOwner owner) throws SQLException {
        if (table != null) {
            table.drop(session, this, owner);
            table = null;
        } else
            ServerLoggers.assertLog(false, "TABLE WAS DROPPED BEFORE");
    }

    public ImCol<ImMap<V, Object>> read(DataSession session, ImMap<K, DataObject> mapValues) throws SQLException, SQLHandledException {
        return read(mapValues, session.sql, session.env, MapFact.<V, Boolean>EMPTYORDER(), 0).values();
    }

    public ImCol<ImMap<V, Object>> read(SQLSession sql, QueryEnvironment env, ImMap<K, DataObject> mapValues) throws SQLException, SQLHandledException {
        return read(mapValues, sql, env, MapFact.<V, Boolean>EMPTYORDER(), 0).values();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(DataSession session) throws SQLException, SQLHandledException {
        return read(session.sql, session.env, MapFact.<V, Boolean>EMPTYORDER());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(SQLSession session, QueryEnvironment env, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return read(MapFact.<K, DataObject>EMPTY(), session, env, orders, 0);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(SQLSession session, QueryEnvironment env, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException, SQLHandledException {
        return read(MapFact.<K, DataObject>EMPTY(), session, env, orders, selectTop);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> read(ImMap<K, DataObject> mapValues, SQLSession session, QueryEnvironment env, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException, SQLHandledException {
        return getQuery(mapValues).execute(session, orders, selectTop, env);
    }

    public Query<K, V> getQuery() {
        return getQuery(MapFact.EMPTY()).getQuery();
    }

    private Query<K, V> getQuery(ImMap<K, DataObject> mapValues) {
        QueryBuilder<K, V> query = new QueryBuilder<>(mapKeys.valuesSet(), mapValues);
        Join<V> tableJoin = join(query.getMapExprs());
        query.addProperties(tableJoin.getExprs());
        query.and(tableJoin.getWhere());
        return query.getQuery();
    }

    public ImSet<Object> readDistinct(V prop, SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        ImRevMap<K, KeyExpr> mapKeys = getMapKeys();
        KeyExpr key = new KeyExpr("key");
        Expr groupExpr = GroupExpr.create(MapFact.singleton("key", join(mapKeys).getExpr(prop)), ValueExpr.TRUE, GroupType.LOGICAL(), MapFact.<String, Expr>singleton("key", key));
        return new Query<>(MapFact.singletonRev("key", key), groupExpr.getWhere()).execute(session, owner).keyOrderSet().getSet().mapSetValues(ImMap::singleValue);
    }

    public <B> ClassWhere<B> getClassWhere(V property, ImRevMap<K, ? extends B> remapKeys, B mapProp) {
        ClassWhere<Field> classWhere = table.getClassWhere(getField(property));
        return new ClassWhere<>(classWhere,
                MapFact.addRevExcl(mapKeys.join(remapKeys),
                        mapProps.rightJoin(MapFact.singletonRev(property, mapProp))));
    }

    public <B> ClassWhere<B> getClassWhere(ImRevMap<K, B> remapKeys) {
        return new ClassWhere<>(table.getClassWhere(), mapKeys.join(remapKeys));
    }


    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    public SessionData saveData() {
        return table;
    }
    // предварительно таблица drop'ается в rollback (вызов dropTables, или явный drop перед вызовом)
    public void rollData(SQLSession sql, SessionData table, OperationOwner owner) throws SQLException {
        assert this.table == null;
        this.table = table;
        this.table.rollDrop(sql, this, owner, true);
    }
    
    public static <T> ImMap<T, SessionData> saveData(Map<T, ? extends SessionTableUsage> map) {
        return MapFact.<T, SessionTableUsage>fromJavaMap(map).mapValues(value -> value.saveData());
    }

    public long getCount() {
        return table.getCount();
    }

    @Override
    public String toString() {
        return "SU@" + System.identityHashCode(this) + " " + table.toString() + " " + getCount() + " " + debugInfo;
    }

    // modifier параметр избыточен (его можно получать из classChanges, но для оптимизации не будем его пересоздавать)
    public ModifyResult updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        try {
            if(!fullHasClassChanges(session)) // optimization 
                return ModifyResult.NO;

            SessionData<?> newTable = table;
            if (this instanceof PropertyChangeTableUsage && hasCorrelations()) // тут делаем по аналогии с updateCurrentClasses (то есть берем expr с учетом modifier предполагая что посли применения у коррелирующего свойства будет такое значение), хотя по идее можно было бы просто после сохранения вызывать ??? (но тут как и сверху неплохо бы проверять только измененные !!!)
                newTable = ((PropertyChangeTableUsage)this).updateCorrelations(newTable, session);

            newTable = newTable.updateCurrentClasses(session);
            return aspectModify(newTable, false); // changed не нужен так как при изменении классов изменится и source
        } catch (Throwable t) {
            aspectException(session.sql, session.env.getOpOwner());
            throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
        }
    }

    protected boolean fullHasClassChanges(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        return table.hasClassChanges(session);
    }
}
