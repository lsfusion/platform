package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;

import static platform.base.BaseUtils.hashEquals;

public class SessionDataTable extends SessionData<SessionDataTable> {
    private SessionTable table;

    private ImOrderSet<KeyField> keys; // чисто для порядка ключей

    private ImMap<KeyField, DataObject> keyValues;
    private ImMap<PropertyField, ObjectValue> propertyValues;

    public SessionDataTable(SessionTable table, ImOrderSet<KeyField> keys, ImMap<KeyField, DataObject> keyValues, ImMap<PropertyField, ObjectValue> propertyValues) {
        assert keys.getSet().containsAll(table.getTableKeys());

        this.table = table;

        this.keys = keys;
        this.keyValues = keyValues;

        this.propertyValues = propertyValues;
    }

    public Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {

        final Join<PropertyField> tableJoin = table.join(joinImplement.filterIncl(table.getTableKeys()));
        return new SessionJoin(joinImplement) {
            public Expr getExpr(PropertyField property) {
                ObjectValue propertyValue = propertyValues.get(property);
                if(propertyValue!=null)
                    return propertyValue.getExpr().and(tableJoin.getWhere());
                else
                    return tableJoin.getExpr(property);
            }
            public Where getWhere() {
                return tableJoin.getWhere();
            }
        }.and(CompareWhere.compareValues(joinImplement.filterIncl(keyValues.keys()), keyValues));
    }

    public ImOrderSet<KeyField> getOrderKeys() {
        return keys;
    }

    public ImSet<PropertyField> getProperties() {
        return table.getProperties().addExcl(propertyValues.keys());
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashValues hashValues) {
        int hash = table.hashValues(hashValues);
        hash += 31 * (MapValuesIterable.hash(keyValues, hashValues) ^ MapValuesIterable.hash(propertyValues, hashValues));
        return hash;
    }

    public ImSet<Value> getValues() {
        return MapValuesIterable.getContextValues(keyValues).merge(MapValuesIterable.getContextValues(propertyValues)).addExcl(table);
    }

    protected SessionDataTable translate(MapValuesTranslate mapValues) {
        return new SessionDataTable(table.translateValues(mapValues), keys,
                mapValues.translateValues(keyValues), mapValues.translateValues(propertyValues));
    }

    public boolean twins(TwinImmutableObject obj) {
        return keys.equals(((SessionDataTable) obj).keys) && table.equals(((SessionDataTable) obj).table) && keyValues.equals(((SessionDataTable) obj).keyValues);
    }

    public SessionDataTable modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, Object owner) throws SQLException {

        ImMap<KeyField, DataObject> fixedKeyValues;
        ImMap<PropertyField, ObjectValue> fixedPropValues;
        SessionTable fixedTable;

        if(type == Modify.DELETE) {
            if(!keyFields.filterIncl(keyValues.keys()).equals(keyValues)) //если константная часть ключа не равна, то нечего удалять
                return this;
            fixedKeyValues = keyValues;
            fixedPropValues = propertyValues;
            fixedTable = table;
        } else {
            fixedKeyValues = keyFields.addEquals(keyValues);
            fixedPropValues = propFields.addEquals(propertyValues);
            fixedTable = table.addFields(session, keys.removeOrder(fixedKeyValues.keys()), keyValues.remove(fixedKeyValues.keys()), propertyValues.remove(fixedPropValues.keys()), owner);
        }
        return new SessionDataTable(fixedTable.modifyRecord(session, keyFields.remove(fixedKeyValues.keys()), propFields.remove(fixedPropValues.keys()), type, owner),
                keys, fixedKeyValues, fixedPropValues);
    }

    @Override
    public SessionData modifyRows(SQLSession session, IQuery<KeyField, PropertyField> query, BaseClass baseClass, Modify type, QueryEnvironment env, Object owner) throws SQLException {
        if(keyValues.isEmpty() && propertyValues.isEmpty() && (type== Modify.LEFT || type== Modify.ADD || type==Modify.DELETE)) // если и так все различны, то не зачем проверять разновидности, добавлять поля и т.п.
            return new SessionDataTable(table.modifyRows(session, query, type, env, owner), keys, keyValues, propertyValues);
        return super.modifyRows(session, query, baseClass, type, env, owner);
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, Pair<Integer,Integer>[] shifts) throws SQLException {
        if(propertyValues.containsKey(property))
            return new SessionDataTable(table, keys, keyValues, SessionRows.updateAdded(propertyValues, property, shifts));
        else {
            table.updateAdded(session, baseClass, property, shifts);
            return this;
        }
    }

    // для оптимизации групповых добавлений (batch processing'а)
    public SessionDataTable(SQLSession session, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {

        this.keys = keys;
        // сначала пробежим по всем проверим с какими field'ами создавать таблицы и заодно propertyClasses узнаем, после этого batch'ем запишем
        keyValues = rows.getKey(0);
        propertyValues = rows.getValue(0);

        for(int i=1,size=rows.size();i<size;i++) {
            keyValues = keyValues.removeNotEquals(rows.getKey(i));
            propertyValues = propertyValues.removeNotEquals(rows.getValue(i));
        }

        final ImSet<KeyField> removeKeys = keyValues.keys(); final ImSet<PropertyField> removeProperties = propertyValues.keys();
        // пробежим по всем вырежем equals и создадим classes
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> tableRows = rows.mapKeyValues(new GetValue<ImMap<KeyField, DataObject>, ImMap<KeyField, DataObject>>() {
            public ImMap<KeyField, DataObject> getMapValue(ImMap<KeyField, DataObject> value) {
                return value.remove(removeKeys);
            }}, new GetValue<ImMap<PropertyField, ObjectValue>, ImMap<PropertyField, ObjectValue>>() {
            public ImMap<PropertyField, ObjectValue> getMapValue(ImMap<PropertyField, ObjectValue> value) {
                return value.remove(removeProperties);
            }});
        table = SessionTable.create(session, keys.removeOrder(removeKeys), properties.remove(removeProperties), tableRows, owner);
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        table.drop(session, owner);
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
        table.rollDrop(session, owner);
    }

    public boolean used(IQuery<?, ?> query) {
        return query.getInnerValues().contains(table);
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
        if(propertyValues.size()>0 && propertyValues.singleValue() instanceof NullValue &&
                !hashEquals(table, fixTable = table.fixKeyClasses(fixClasses))) {
            return new SessionDataTable(fixTable, keys, keyValues, propertyValues);
        } else
            return this;
    }

    public SessionData updateCurrentClasses(DataSession session) throws SQLException {
        return new SessionDataTable(table.updateCurrentClasses(session), keys, session.updateCurrentClasses(keyValues), session.updateCurrentClasses(propertyValues));
    }

    public boolean isEmpty() {
        return false;
    }

    public int getCount() {
        return table.count;
    }
}
