package platform.server.data;

import platform.base.Pair;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.expr.where.extra.CompareWhere;
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

public class SessionRows extends SessionData<SessionRows> {

    public final static int MAX_ROWS = 1;

    private ImOrderSet<KeyField> keys;
    private ImSet<PropertyField> properties;

    protected final ImMap<ImMap<KeyField,DataObject>,ImMap<PropertyField,ObjectValue>> rows;

    public SessionRows(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) {
        this(keys, properties, MapFact.<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>EMPTY());
    }

    public SessionRows(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) {
        this.keys = keys;
        this.properties = properties;
        this.rows = rows;
    }

    public ImOrderSet<KeyField> getOrderKeys() {
        return keys;
    }

    public ImSet<PropertyField> getProperties() {
        return properties;
    }

    public Join<PropertyField> join(ImMap<KeyField, ? extends Expr> joinImplement) {
        return new SessionJoin(joinImplement) {
            public Expr getExpr(PropertyField property) {
                CaseExprInterface cases = Expr.newCases(true);
                for(int i=0,size=rows.size();i<size;i++)
                    cases.add(CompareWhere.compareValues(joinImplement,rows.getKey(i)),rows.getValue(i).get(property).getExpr());
                return cases.getFinal();
            }

            public Where getWhere() {
                Where result = Where.FALSE;
                for(int i=0,size=rows.size();i<size;i++)
                    result = result.or(CompareWhere.compareValues(joinImplement,rows.getKey(i)));
                return result;
            }
        };
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=rows.size();i<size;i++)
            hash += MapValuesIterable.hash(rows.getKey(i),hashValues) ^ MapValuesIterable.hash(rows.getValue(i),hashValues);
        return hash;
    }

    public ImSet<Value> getValues() {
        MSet<Value> result = SetFact.mSet();
        for(int i=0,size=rows.size();i<size;i++) {
            result.addAll(MapValuesIterable.getContextValues(rows.getKey(i)));
            result.addAll(MapValuesIterable.getContextValues(rows.getValue(i)));
        }
        return result.immutable();
    }

    protected SessionRows translate(final MapValuesTranslate mapValues) {
        return new SessionRows(keys, properties, mapValues.translateMapKeyValues(rows));
    }

    public boolean twins(TwinImmutableObject obj) {
        return keys.equals(((SessionRows)obj).keys) && properties.equals(((SessionRows)obj).properties) && rows.equals(((SessionRows)obj).rows);
    }

    public SessionData modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, Object owner) throws SQLException {

        if(type==Modify.DELETE)
            return new SessionRows(keys, properties, rows.remove(keyFields));
        if(type== Modify.LEFT)
            if(rows.containsKey(keyFields))
                return this;
        if(type== Modify.ADD)
            if(rows.containsKey(keyFields))
                throw new RuntimeException("should not be");
        if(type== Modify.UPDATE)
            if(!rows.containsKey(keyFields))
                return this;

        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> orRows = rows.override(keyFields, propFields);

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

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses(ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) {
        Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses = new Pair<ClassWhere<KeyField>, ImMap<PropertyField,ClassWhere<Field>>>(ClassWhere.<KeyField>FALSE(), properties.toMap(ClassWhere.<Field>FALSE()));
        ImSet<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>> rowClasses = rows.mapMergeSetValues(new GetKeyValue<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>, ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>() {
            public Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> getMapValue(ImMap<KeyField, DataObject> key, ImMap<PropertyField, ObjectValue> value) {
                return new Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>(DataObject.getMapClasses(key), ObjectValue.getMapClasses(value));
            }});
        for(int i=0,size=rowClasses.size();i<size;i++) {
            Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> classes = rowClasses.get(i);
            orClasses = SessionTable.orFieldsClassWheres(classes.first, classes.second, orClasses.first, orClasses.second);
        }
        return orClasses;
    }

    @IdentityLazy
    private Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses() {
        return getClasses(properties, rows);
    }

    @Override
    public ClassWhere<KeyField> getClassWhere() {
        return getClasses().first;
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        if(rows.size()==0)
            return ClassWhere.FALSE();
        else
            return getClasses().second.get(property);
    }

    public SessionRows fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        return this;
    }

    public SessionRows updateCurrentClasses(final DataSession session) throws SQLException {
        MExclMap<ImMap<KeyField, DataObject>,ImMap<PropertyField, ObjectValue>> mUpdatedRows = MapFact.mExclMap(rows.size());// exception кидается
        for(int i=0,size=rows.size();i<size;i++)
            mUpdatedRows.exclAdd(session.updateCurrentClasses(rows.getKey(i)), session.updateCurrentClasses(rows.getValue(i)));
        return new SessionRows(keys, properties, mUpdatedRows.immutable());
    }

    public boolean isEmpty() {
        return rows.size()==0;
    }

    public int getCount() {
        return rows.size();
    }

    // assert что содержит
    public static ImMap<PropertyField, ObjectValue> updateAdded(ImMap<PropertyField, ObjectValue> map, PropertyField property, int count) {
        DataObject prevValue = (DataObject)map.get(property);
        return map.replaceValue(property, new DataObject(ObjectType.idClass.read(prevValue.object) + count, prevValue.objectClass));
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, final PropertyField property, final int count) {
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> updatedRows = rows.mapValues(new GetValue<ImMap<PropertyField, ObjectValue>, ImMap<PropertyField, ObjectValue>>() {
            public ImMap<PropertyField, ObjectValue> getMapValue(ImMap<PropertyField, ObjectValue> value) {
                return updateAdded(value, property, count);
            }});
        return new SessionRows(keys, properties, updatedRows);
    }
}
