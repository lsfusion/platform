package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.lambda.Processor;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.InnerContext;
import lsfusion.server.data.caches.MapValuesIterable;
import lsfusion.server.data.caches.hash.HashValues;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.IsClassType;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;

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
                CaseExprInterface cases = Expr.newCases(true, rows.size());
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
    public int hash(HashValues hashValues) {
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

    public boolean calcTwins(TwinImmutableObject obj) {
        return keys.equals(((SessionRows)obj).keys) && properties.equals(((SessionRows)obj).properties) && rows.equals(((SessionRows)obj).rows);
    }

    public SessionData modifyRecord(SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, TableOwner owner, OperationOwner opOwner, Result<Boolean> changed) throws SQLException, SQLHandledException {

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
            return new SessionDataTable(session, keys, properties, orRows, owner, opOwner);
        else
            return new SessionRows(keys, properties, orRows);
    }

    public void drop(SQLSession session, TableOwner owner, OperationOwner opOwner) {
    }
    public void rollDrop(SQLSession session, TableOwner owner, OperationOwner opOwner, boolean assertNotExists) throws SQLException {
    }

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Rows :" + rows);
    }

    public void outClasses(SQLSession session, BaseClass baseClass, Processor<String> processor) throws SQLException, SQLHandledException {
        processor.proceed("Rows :" + rows);
    }

    public boolean used(InnerContext query) {
        return false;
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses(ImSet<PropertyField> properties, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) {
        Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses = new Pair<>(ClassWhere.<KeyField>FALSE(), properties.toMap(ClassWhere.<Field>FALSE()));
        ImSet<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>> rowClasses = rows.mapMergeSetValues(new GetKeyValue<Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>>, ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>() {
            public Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> getMapValue(ImMap<KeyField, DataObject> key, ImMap<PropertyField, ObjectValue> value) {
                return new Pair<>(DataObject.getMapDataClasses(key), ObjectValue.getMapClasses(value));
            }});
        for(int i=0,size=rowClasses.size();i<size;i++) {
            Pair<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> classes = rowClasses.get(i);
            orClasses = SessionTable.orFieldsClassWheres(classes.first, classes.second, orClasses.first, orClasses.second);
        }
        return orClasses;
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getClasses(ImMap<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> rows, ImSet<PropertyField> properties) {
        Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses = new Pair<>(ClassWhere.<KeyField>FALSE(), properties.toMap(ClassWhere.<Field>FALSE()));
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

    public ImMap<PropertyField, ClassWhere<Field>> getPropertyClasses() {
        return getClasses().second;
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        if(rows.size()==0)
            return ClassWhere.FALSE();
        else
            return getClasses().second.get(property);
    }

    public SessionRows fixKeyClasses(ClassWhere<KeyField> fixClasses, PropertyField valueField) {
        return this;
    }

    @Override
    public boolean hasClassChanges(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        for(int i=0,size=rows.size();i<size;i++)
            if(session.hasClassChanges(rows.getKey(i)) || session.hasClassChanges(rows.getValue(i)))
                return true;
        return false;
    }

    public SessionRows updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
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
    public static ImMap<PropertyField, ObjectValue> updateAdded(ImMap<PropertyField, ObjectValue> map, PropertyField property, Pair<Long, Long>[] shifts) {
        ObjectValue value = map.get(property);
        if(value instanceof DataObject) {
            DataObject dataValue = (DataObject) value;
            Long read = ObjectType.idClass.read(dataValue.object);
            assert shifts.length > 0;
            long calcshift = 0; long aggsh = 0;

            for(Pair<Long, Long> shift : shifts) { // по аналогии с updateAdded в таблицах
                if(read > aggsh)
                    calcshift = shift.first;
                aggsh += shift.second;
            }
            return map.replaceValue(property, new DataObject(read + calcshift, (ConcreteObjectClass) dataValue.objectClass));
        }
        return map;
    }

    @Override
    public SessionData updateAdded(SQLSession session, BaseClass baseClass, final PropertyField property, final Pair<Long, Long>[] shifts, OperationOwner owner, TableOwner tableOwner) {
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

    public static <O extends ObjectValue> boolean checkClasses(O value, SQLSession session, BaseClass baseClass, OperationOwner owner, ValueClass inconsistentTableClass, Result<Boolean> rereadChange, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        boolean result = false;
        rereadChange.set(false);
        if(value instanceof DataObject) {
            DataObject dataValue = (DataObject) value;
            ConcreteClass objectClass = dataValue.objectClass;
            if(objectClass instanceof ConcreteObjectClass) {
                ConcreteObjectClass concreteObjectClass = (ConcreteObjectClass) objectClass;
                if(Table.checkClasses(concreteObjectClass, (CustomClass)inconsistentTableClass, rereadChange, classRemove, timestamp)) {
                    assert concreteObjectClass instanceof ConcreteCustomClass; // иначе checkClasses не прошел бы
                    result = true;
                }
            }
        }
        return result;
    }
    
    public static <F extends Field, O extends ObjectValue> ImMap<F, O> checkClasses(final ImMap<F, O> map, SQLSession session, final BaseClass baseClass, OperationOwner owner, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> rRereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        ImFilterValueMap<F, DataObject> mToCheckMap = map.mapFilterValues(); // exception + второй результат
        MExclSet<Field> mRereadChanges = SetFact.mExclSetMax(map.size());
        for(int i=0,size=map.size();i<size;i++) {
            F field = map.getKey(i);
            O value = map.getValue(i);
            
            Result<Boolean> rereadValue = new Result<>();
            ValueClass inconsistentTableClass = inconsistentTableClasses.get(field);
            if(inconsistentTableClass != null && checkClasses(value, session, baseClass, owner, inconsistentTableClass, rereadValue, classRemove, timestamp)) // проверка для correlations
                mToCheckMap.mapValue(i, (DataObject) value);
            if(rereadValue.result)
                mRereadChanges.exclAdd(field);
        }
        ImSet<Field> rereadChanges = mRereadChanges.immutable();
        if(rRereadChanges.result != null)
            rereadChanges = rereadChanges.merge(rRereadChanges.result);
        rRereadChanges.set(rereadChanges);
        final ImMap<F, DataObject> toCheckMap = mToCheckMap.immutableValue();
        
        if(toCheckMap.isEmpty())
            return map;
        
        ImMap<F, Expr> classExprs = toCheckMap.mapValues(new GetValue<Expr, DataObject>() {
            @Override
            public Expr getMapValue(DataObject value) {
                ConcreteObjectClass objectClass = (ConcreteObjectClass) value.objectClass;
                return value.getInconsistentExpr().classExpr(objectClass.getValueClassSet(), IsClassType.INCONSISTENT);// тут (как и в Table.readClasses) не совсем корректно брать текущий класс, но проверять куда может измениться этот класс (то есть baseClass использовать) убьет производительность
            }
        });

        // тут можно было бы делать как в Table.readClasses, -2, -1, 0 но это сложнее реализовывать и в общем то нужно только для оптимизации, так как rereadChanges - удалит изменение (то есть -2 само обработает)
        ImMap<F, Object> classValues = Expr.readValues(session, classExprs, owner);
        final Result<Boolean> updated = new Result<>(false);
        ImMap<F, DataObject> checkedMap = classValues.mapItValues(new GetKeyValue<DataObject, F, Object>() {
            @Override
            public DataObject getMapValue(F key, Object value) {
                ConcreteObjectClass newConcreteClass = baseClass.findConcreteClassID((Long) value);
                DataObject dataObject = toCheckMap.get(key);
                if (BaseUtils.hashEquals(newConcreteClass, dataObject.objectClass))
                    return dataObject;
                updated.set(true);
                return new DataObject((Long)dataObject.object, newConcreteClass);
            }
        });

        if(!updated.result) // самый частый случай
            return map;
        return map.overrideIncl(BaseUtils.<ImMap<F, O>>immutableCast(checkedMap)); // assert Incl
    }
    
    public static Pair<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> checkClasses(ImMap<KeyField, DataObject> keys, ImMap<PropertyField, ObjectValue> props, SQLSession session, BaseClass baseClass, OperationOwner owner, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> rRereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        ImMap<Field, ObjectValue> fields = MapFact.addExcl(keys, props);
        ImMap<Field, ObjectValue> updatedFields = checkClasses(fields, session, baseClass, owner, inconsistentTableClasses, rRereadChanges, classRemove, timestamp);

        if(BaseUtils.hashEquals(fields, updatedFields))
            return new Pair<>(keys, props);
        return new Pair<>(BaseUtils.<ImMap<KeyField, DataObject>>immutableCast(updatedFields.filterIncl(keys.keys())), updatedFields.filterIncl(props.keys()));
    }

    public SessionRows checkClasses(SQLSession session, BaseClass baseClass, boolean updateClasses, OperationOwner owner, boolean inconsistent, ImMap<Field, ValueClass> inconsistentTableClasses, Result<ImSet<Field>> rereadChanges, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        if(!inconsistent)
            return this;

        rereadChanges.set(SetFact.<Field>EMPTY());
        MExclMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> mUpdatedRows = MapFact.mExclMap(rows.size()); // excl
        for(int i=0,size=rows.size();i<size;i++) {
            Pair<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> updatedRow = 
                    checkClasses(rows.getKey(i), rows.getValue(i), session, baseClass, owner, inconsistentTableClasses, rereadChanges, classRemove, timestamp);
            mUpdatedRows.exclAdd(updatedRow.first, updatedRow.second);
        }
        ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> updatedRows = mUpdatedRows.immutable();
        
        if(BaseUtils.hashEquals(updatedRows, rows))
            return this;
        else
            return new SessionRows(keys, properties, updatedRows);
    }

    public String getQuerySource(final SQLSyntax syntax, final ImOrderSet<Field> orderedFields) {
        return rows.toString(new GetKeyValue<String, ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>>() {
            public String getMapValue(ImMap<KeyField, DataObject> key, ImMap<PropertyField, ObjectValue> value) {
                return "(" +
                        orderedFields.mapList(MapFact.addExcl(key, value)).toString(new GetValue<String, ObjectValue>() {
                            public String getMapValue(ObjectValue value) {
                                assert value.isSafeString(syntax);
                                return value.getString(syntax);
                            }
                        }, ",")        
                        +
                        ")";
            }
        }, ",");
    }
}
