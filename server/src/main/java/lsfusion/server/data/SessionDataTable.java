package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.InnerContext;
import lsfusion.server.caches.MapValuesIterable;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.hashEquals;

public class SessionDataTable extends SessionData<SessionDataTable> {
    private final SessionTable table;

    private final ImOrderSet<KeyField> keys; // чисто для порядка ключей

    private final ImMap<KeyField, DataObject> keyValues;
    private final ImMap<PropertyField, ObjectValue> propertyValues;

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

    public boolean calcTwins(TwinImmutableObject obj) {
        return keys.equals(((SessionDataTable) obj).keys) && table.equals(((SessionDataTable) obj).table) && keyValues.equals(((SessionDataTable) obj).keyValues);
    }

    public SessionDataTable modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, TableOwner owner, OperationOwner opOwner, Result<Boolean> changed) throws SQLException, SQLHandledException {

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
            fixedTable = table.addFields(session, keys.removeOrder(fixedKeyValues.keys()), keyValues.remove(fixedKeyValues.keys()), propertyValues.remove(fixedPropValues.keys()), owner, opOwner);
        }
        return new SessionDataTable(fixedTable.modifyRecord(session, keyFields.remove(fixedKeyValues.keys()), propFields.remove(fixedPropValues.keys()), type, owner, opOwner, changed),
                keys, fixedKeyValues, fixedPropValues);
    }

    @Override
    public SessionData modifyRows(SQLSession session, IQuery<KeyField, PropertyField> query, BaseClass baseClass, Modify type, QueryEnvironment env, TableOwner owner, Result<Boolean> changed, boolean updateClasses) throws SQLException, SQLHandledException {
        if(keyValues.isEmpty() && propertyValues.isEmpty() && (type == Modify.LEFT || type== Modify.ADD || type==Modify.DELETE || (Settings.get().isModifySessionTableInsteadOfRewrite() && !used(query)))) // если и так все различны, то не зачем проверять разновидности, добавлять поля и т.п.
            return new SessionDataTable(table.modifyRows(session, query, type, env, owner, changed, updateClasses), keys, keyValues, propertyValues);
        return super.modifyRows(session, query, baseClass, type, env, owner, changed, updateClasses);
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, Pair<Integer, Integer>[] shifts, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        if(propertyValues.containsKey(property))
            return new SessionDataTable(table, keys, keyValues, SessionRows.updateAdded(propertyValues, property, shifts));
        else {
            table.updateAdded(session, baseClass, property, shifts, owner, tableOwner);
            return this;
        }
    }

    // для оптимизации групповых добавлений (batch processing'а)
    public SessionDataTable(SQLSession session, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, TableOwner owner, OperationOwner opOwner) throws SQLException, SQLHandledException {

        this.keys = keys;
        // сначала пробежим по всем проверим с какими field'ами создавать таблицы и заодно propertyClasses узнаем, после этого batch'ем запишем
        ImMap<KeyField, DataObject> keyValues = rows.getKey(0);
        ImMap<PropertyField, ObjectValue> propertyValues = rows.getValue(0);

        for(int i=1,size=rows.size();i<size;i++) {
            keyValues = keyValues.mergeEqualsIncl(rows.getKey(i));
            propertyValues = propertyValues.mergeEqualsIncl(rows.getValue(i));
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
        this.keyValues = keyValues;
        this.propertyValues = propertyValues;
        table = SessionTable.create(session, keys.removeOrder(removeKeys), properties.remove(removeProperties), tableRows, owner, opOwner);
    }

    public void drop(SQLSession session, TableOwner owner, OperationOwner opOwner) throws SQLException {
        table.drop(session, owner, opOwner);
    }
    public void rollDrop(SQLSession session, TableOwner owner, OperationOwner opOwner) throws SQLException {
        table.rollDrop(session, owner, opOwner);
    }

    public boolean used(InnerContext query) {
        return query.getInnerValues().contains(table);
    }

    @Override
    public void out(SQLSession session) throws SQLException, SQLHandledException {
        System.out.println("Key Values : " + keyValues);
        System.out.println("Prop Values : " + propertyValues);
        table.out(session);
    }

    private ClassWhere<KeyField> getKeyValueClasses() {
        return new ClassWhere<KeyField>(DataObject.getMapDataClasses(keyValues));
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
                !hashEquals(table, fixTable = table.fixKeyClasses(fixClasses.remove(keyValues.keys())))) {
            return new SessionDataTable(fixTable, keys, keyValues, propertyValues);
        } else
            return this;
    }

    public SessionData updateCurrentClasses(DataSession session) throws SQLException, SQLHandledException {
        return new SessionDataTable(table.updateCurrentClasses(session), keys, session.updateCurrentClasses(keyValues), session.updateCurrentClasses(propertyValues));
    }

    public boolean isEmpty() {
        return false;
    }

    public int getCount() {
        return table.count;
    }

    @Override
    public String toString() {
        return table + "{k:" + keyValues + ",v:" + propertyValues + "}";
    }

    public SessionDataTable checkClasses(SQLSession session, BaseClass baseClass, boolean updateClasses, OperationOwner owner) throws SQLException, SQLHandledException {
        SessionTable checkTable = table.checkClasses(session, baseClass, updateClasses, owner);
        if(BaseUtils.hashEquals(checkTable, table))
            return this;
        else
            return new SessionDataTable(checkTable, keys, keyValues, propertyValues);
    }
}
