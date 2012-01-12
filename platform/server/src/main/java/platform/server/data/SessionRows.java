package platform.server.data;

import platform.base.*;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

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

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {
        return new AbstractJoin<PropertyField>() {

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

            public Collection<PropertyField> getProperties() {
                return properties;
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

    public SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException {

        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> orRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Map<PropertyField, ObjectValue> prevValue = orRows.put(keyFields,propFields);
        assert update || prevValue==null;

        if(orRows.size()>MAX_ROWS) // если превысили количество рядов "переходим" в таблицу
            return new SessionDataTable(session, keys, properties, orRows, groupLast, owner);
        else
            return new SessionRows(keys, properties, orRows);
    }

    public void drop(SQLSession session, Object owner) {
    }

    public SessionData deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException {
        return new SessionRows(this.keys, properties, BaseUtils.removeKey(rows, keys));
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getKey().get(mapField).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, removeRows);
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getValue().get(property).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, removeRows);
    }

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Rows :" + rows);
    }

    public boolean used(Query<?, ?> query) {
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

    public boolean isEmpty() {
        return rows.size()==0;
    }
}
