package platform.server.data;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class SessionDataTable extends SessionData<SessionDataTable> {
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

    public Join<PropertyField> join(Map<KeyField, ? extends Expr> joinImplement) {

        final Join<PropertyField> tableJoin = table.join(filterKeys(joinImplement, table.keys));
        return new SessionJoin(joinImplement) {
            public Expr getExpr(PropertyField property) {
                ObjectValue propertyValue = propertyValues.get(property);
                if(propertyValue!=null)
                    return propertyValue.getExpr();
                else
                    return tableJoin.getExpr(property);
            }
            public Where getWhere() {
                return tableJoin.getWhere();
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

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashValues hashValues) {
        int hash = table.hashValues(hashValues);
        hash += 31 * (MapValuesIterable.hash(keyValues, hashValues) ^ MapValuesIterable.hash(propertyValues, hashValues));
        return hash;
    }

    public QuickSet<Value> getValues() {
        return MapValuesIterable.getContextValues(keyValues).merge(MapValuesIterable.getContextValues(propertyValues)).merge(table);
    }

    protected SessionDataTable translate(MapValuesTranslate mapValues) {
        return new SessionDataTable(table.translateValues(mapValues), keys,
                mapValues.translateValues(keyValues), mapValues.translateValues(propertyValues));
    }

    public boolean twins(TwinImmutableInterface obj) {
        return keys.equals(((SessionDataTable) obj).keys) && table.equals(((SessionDataTable) obj).table) && keyValues.equals(((SessionDataTable) obj).keyValues);
    }

    public SessionDataTable insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, Object owner) throws SQLException {
        
        Map<KeyField, DataObject> fixedKeyValues = BaseUtils.mergeEquals(keyFields, keyValues);
        Map<PropertyField, ObjectValue> fixedPropValues = BaseUtils.mergeEquals(propFields, propertyValues);

        return new SessionDataTable(table.addFields(session, BaseUtils.filterNotList(keys, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(keyValues, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(propertyValues, fixedPropValues.keySet()), owner).
              insertRecord(session, BaseUtils.filterNotKeys(keyFields, fixedKeyValues.keySet()), BaseUtils.filterNotKeys(propFields, fixedPropValues.keySet()), update, owner),
                keys, fixedKeyValues, fixedPropValues);
    }

    @Override
    public SessionData addRows(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, boolean update, QueryEnvironment env, Object owner) throws SQLException {
        if(keyValues.isEmpty() && propertyValues.isEmpty() && !update) // если и так все различны, то не зачем проверять разновидности, добавлять поля и т.п.
            return new SessionDataTable(table.addRows(session, query, update, env, owner), keys, keyValues, propertyValues);
        return super.addRows(session, query, baseClass, update, env, owner);
    }

    // для оптимизации групповых добавлений (batch processing'а)
    public SessionDataTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {

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
        table = SessionTable.create(session, BaseUtils.filterNotList(keys, keyValues.keySet()), BaseUtils.removeSet(properties, propertyValues.keySet()), tableRows, owner);
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        table.drop(session, owner);
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
        table.rollDrop(session, owner);
    }

    public boolean used(Query<?, ?> query) {
        return query.getInnerValues().contains(table);
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

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Key Values : " + keyValues);
        System.out.println("Prop Values : " + propertyValues);
        table.out(session);
    }

    private ClassWhere<KeyField> getKeyValueClasses() {
        return new ClassWhere<KeyField>(DataObject.getMapClasses(keyValues));
    }

    public ClassWhere<KeyField> getClassWhere() {
        return table.getClasses().and(getKeyValueClasses());
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        ClassWhere<Field> propClasses;
        ObjectValue<?> objectValue = propertyValues.get(property);
        if(objectValue!=null)
            propClasses = objectValue.<Field>getClassWhere(property).and(BaseUtils.<ClassWhere<Field>>immutableCast(table.getClasses()));
        else
            propClasses = table.getClassWhere(property);
        return propClasses.and(BaseUtils.<ClassWhere<Field>>immutableCast(getKeyValueClasses()));
    }

    // см. usage
    public SessionDataTable fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        assert getProperties().size()==1;
        SessionTable fixTable;
        if(propertyValues.size()>0 && BaseUtils.singleValue(propertyValues) instanceof NullValue &&
                !hashEquals(table, fixTable = table.fixKeyClasses(fixClasses))) {
            return new SessionDataTable(fixTable, keys, keyValues, propertyValues);
        } else
            return this;
    }

    public boolean isEmpty() {
        return false;
    }
}
