package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.InnerContext;
import lsfusion.server.caches.MapValuesIterable;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;

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

    public SessionData modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, Object owner, Result<Boolean> changed) throws SQLException, SQLHandledException {

        if(type==Modify.DELETE)
            return new SessionRows(keys, properties, rows.remove(keyFields));
        if(type== Modify.LEFT)
            if(rows.containsKey(keyFields))
                return this;
        assert !(type== Modify.ADD && rows.containsKey(keyFields));
        if(type== Modify.UPDATE)
            if(!rows.containsKey(keyFields))
                return this;

        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> orRows;
        if(type==Modify.ADD || type==Modify.LEFT) {
            changed.set(true);
            orRows = rows.addExcl(keyFields, propFields);
        } else {
            orRows = rows.override(keyFields, propFields);
            if(!BaseUtils.hashEquals(orRows, rows))
                changed.set(true);
        }

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

    public boolean used(InnerContext query) {
        return false;
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses(ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) {
        Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses = new Pair<ClassWhere<KeyField>, ImMap<PropertyField,ClassWhere<Field>>>(ClassWhere.<KeyField>FALSE(), properties.toMap(ClassWhere.<Field>FALSE()));
        ImSet<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>> rowClasses = rows.mapMergeSetValues(new GetKeyValue<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>, ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>() {
            public Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> getMapValue(ImMap<KeyField, DataObject> key, ImMap<PropertyField, ObjectValue> value) {
                return new Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>(DataObject.getMapDataClasses(key), ObjectValue.getMapClasses(value));
            }});
        for(int i=0,size=rowClasses.size();i<size;i++) {
            Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> classes = rowClasses.get(i);
            orClasses = SessionTable.orFieldsClassWheres(classes.first, classes.second, orClasses.first, orClasses.second);
        }
        return orClasses;
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses(ImMap<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> rows, ImSet<PropertyField> properties) {
        Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses = new Pair<ClassWhere<KeyField>, ImMap<PropertyField,ClassWhere<Field>>>(ClassWhere.<KeyField>FALSE(), properties.toMap(ClassWhere.<Field>FALSE()));
        for(int i=0,size=rows.size();i<size;i++)
            orClasses = SessionTable.orFieldsClassWheres(rows.getKey(i), rows.getValue(i), orClasses.first, orClasses.second);
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

    public SessionRows updateCurrentClasses(final DataSession session) throws SQLException, SQLHandledException {
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
    public static ImMap<PropertyField, ObjectValue> updateAdded(ImMap<PropertyField, ObjectValue> map, PropertyField property, Pair<Integer, Integer>[] shifts) {
        ObjectValue value = map.get(property);
        if(value instanceof DataObject) {
            DataObject dataValue = (DataObject) value;
            Integer read = ObjectType.idClass.read(dataValue.object);
            assert shifts.length > 0;
            int calcshift = 0; int aggsh = 0;

            for(Pair<Integer, Integer> shift : shifts) { // по аналогии с updateAdded в таблицах
                if(read > aggsh)
                    calcshift = shift.first;
                aggsh += shift.second;
            }
            return map.replaceValue(property, new DataObject(read + calcshift, dataValue.objectClass));
        }
        return map;
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, final PropertyField property, final Pair<Integer, Integer>[] shifts) {
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> updatedRows = rows.mapValues(new GetValue<ImMap<PropertyField, ObjectValue>, ImMap<PropertyField, ObjectValue>>() {
            public ImMap<PropertyField, ObjectValue> getMapValue(ImMap<PropertyField, ObjectValue> value) {
                return updateAdded(value, property, shifts);
            }});
        return new SessionRows(keys, properties, updatedRows);
    }

    @Override
    public String toString() {
        return rows.toString();
    }

    public SessionRows checkClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return this;
    }
}
