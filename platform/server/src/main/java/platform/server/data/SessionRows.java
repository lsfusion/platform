package platform.server.data;

import platform.base.*;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class SessionRows extends SessionData<SessionRows> {

    public final static int MAX_ROWS = 1;

    private List<KeyField> keys;
    private Set<PropertyField> properties;

    protected final Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> rows;

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties) {
        this(keys, properties, new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>());
    }

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        this.keys = keys;
        this.properties = properties;
        this.rows = rows;
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

    public Join<PropertyField> join(Map<KeyField, ? extends Expr> joinImplement) {
        return new SessionJoin(joinImplement) {
            public Expr getExpr(PropertyField property) {
                CaseExprInterface cases = Expr.newCases();
                for(Map.Entry<Map<KeyField, DataObject>,Map<PropertyField, ObjectValue>> row : rows.entrySet())
                    cases.add(CompareWhere.compareValues(joinImplement,row.getKey()),row.getValue().get(property).getExpr());
                return cases.getFinal();
            }

            public Where getWhere() {
                Where result = Where.FALSE;
                for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
                    result = result.or(CompareWhere.compareValues(joinImplement,row.getKey()));
                return result;
            }
        };
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
            hash += MapValuesIterable.hash(row.getKey(),hashValues) ^ MapValuesIterable.hash(row.getValue(),hashValues);
        return hash;
    }

    public QuickSet<Value> getValues() {
        QuickSet<Value> result = new QuickSet<Value>();
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet()) {
            result.addAll(MapValuesIterable.getContextValues(row.getKey()));
            result.addAll(MapValuesIterable.getContextValues(row.getValue()));
        }
        return result;
    }

    protected SessionRows translate(MapValuesTranslate mapValues) {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> transRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
            transRows.put(mapValues.translateValues(row.getKey()), mapValues.translateValues(row.getValue()));
        return new SessionRows(keys, properties, transRows);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return keys.equals(((SessionRows)obj).keys) && properties.equals(((SessionRows)obj).properties) && rows.equals(((SessionRows)obj).rows);
    }

    public SessionData modifyRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, Modify type, Object owner) throws SQLException {

        if(type==Modify.DELETE)
            return new SessionRows(keys, properties, BaseUtils.removeKey(rows, keyFields));
        if(type== Modify.LEFT)
            if(rows.containsKey(keyFields))
                return this;
        if(type== Modify.ADD)
            if(rows.containsKey(keyFields))
                throw new RuntimeException("should not be");
        if(type== Modify.UPDATE)
            if(!rows.containsKey(keyFields))
                return this;

        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> orRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        orRows.put(keyFields,propFields);

        if(orRows.size()>MAX_ROWS) // если превысили количество рядов "переходим" в таблицу
            return new SessionDataTable(session, keys, properties, orRows, owner);
        else
            return new SessionRows(keys, properties, orRows);
    }

    public void drop(SQLSession session, Object owner) {
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
    }

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Rows :" + rows);
    }

    public boolean used(IQuery<?, ?> query) {
        return false;
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getClasses(Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = new Pair<ClassWhere<KeyField>, Map<PropertyField,ClassWhere<Field>>>(ClassWhere.<KeyField>STATIC(false), BaseUtils.toMap(properties, ClassWhere.<Field>STATIC(false)));
        for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet())
            orClasses = SessionTable.orFieldsClassWheres(orClasses.first, orClasses.second, row.getKey(), row.getValue());
        return orClasses;
    }

    @IdentityLazy
    private Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getClasses() {
        return getClasses(properties, rows);
    }

    @Override
    public ClassWhere<KeyField> getClassWhere() {
        return getClasses().first;
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        if(rows.size()==0)
            return ClassWhere.STATIC(false);
        else
            return getClasses().second.get(property);
    }

    public SessionRows fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        return this;
    }

    public SessionRows updateCurrentClasses(DataSession session) throws SQLException {
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> updatedRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet())
            updatedRows.put(session.updateCurrentClasses(row.getKey()), session.updateCurrentClasses(row.getValue()));
        return new SessionRows(keys, properties, updatedRows);
    }

    public boolean isEmpty() {
        return rows.size()==0;
    }

    public int getCount() {
        return rows.size();
    }

    // assert что содержит
    public static Map<PropertyField, ObjectValue> updateAdded(Map<PropertyField, ObjectValue> map, PropertyField property, int count) {
        Map<PropertyField, ObjectValue> result = new HashMap<PropertyField, ObjectValue>(map);

        DataObject prevValue = (DataObject)result.get(property);
        result.put(property, new DataObject(ObjectType.idClass.read(prevValue.object) + count, prevValue.objectClass));
        return result;
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, int count) {
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> updatedRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet())
            updatedRows.put(row.getKey(), updateAdded(row.getValue(), property, count));
        return new SessionRows(keys, properties, updatedRows);
    }
}
