package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.caches.hash.HashValues;
import lsfusion.server.classes.*;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.physics.dev.integration.service.*;
import lsfusion.server.logics.action.session.Modifier;
import lsfusion.server.logics.action.session.SessionChanges;
import lsfusion.server.logics.action.session.SessionTableUsage;
import lsfusion.server.logics.action.session.SinglePropertyTableUsage;

import java.sql.SQLException;

public class DataObject extends ObjectValue<DataObject> implements PropertyObjectInterfaceInstance, PropertyObjectInterfaceEntity, ImportKeyInterface, ImportFieldInterface, ImportDeleteInterface {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public DataObject() {

    }

//    public <T extends Number> DataObject(T object, IntegralClass<T> dataClass) {
//        this(object, (DataClass<T>) dataClass);
//    }
//    public DataObject(Timestamp object, DateTimeClass dataClass) {
//        this(object, (DataClass<Timestamp>) dataClass);
//    }
//    public DataObject(Date object, DateClass dataClass) {
//        this(object, (DataClass<Date>) dataClass);
//    }
//    public DataObject(byte[] object, FileClass dataClass) {
//        this(object, (DataClass<byte[]>) dataClass);
//    }
//    public DataObject(String object, StringClass dataClass) {
//        this(object, (DataClass<String>) dataClass);
//    }
    public <T> DataObject(T object, DataClass<T> dataClass) {
        this((Object)object, dataClass);
    }
    public DataObject(Long object, ConcreteObjectClass objectClass) {
        this((Object)object, (ConcreteClass)objectClass);
    }
    public DataObject(Integer object, ConcreteObjectClass objectClass) {
        this((Object)object, (ConcreteClass)objectClass);
        throw new UnsupportedOperationException();// should be long
    }
    public DataObject(Object object, ConcreteClass objectClass) { // желательно использовать верхние конструкторы
        if(objectClass instanceof StringClass || objectClass instanceof NumericClass)
            object = ((DataClass)objectClass).read(object);

        this.object = object;

        assert objectClass.getType().read(object).equals(object); // чтобы читалось то что писалось

        this.objectClass = objectClass;
    }

    public DataObject(String string) {
        this(string, StringClass.getv(string.length()));
    }

    public DataObject(Double dbl) {
        this(dbl, DoubleClass.instance);
    }

    public DataObject(Boolean bl) {
        this(bl, LogicalClass.instance);
    }
    
    public static DataObject TRUE = new DataObject(true);
    public static ObjectValue create(boolean value) {
        return value ? TRUE : NullValue.instance; 
    }

    public DataObject(Integer ig) {
        this(ig, IntegerClass.instance);
    }

    public DataObject(Long ig) {
        this(ig, LongClass.instance);
    }

    // ветки для insert'ов
    public boolean isSafeString(SQLSyntax syntax) {
        return getType().isSafeString(object);
    }
    public String getString(SQLSyntax syntax) {
        return getType().getString(object, syntax);
    }

    // по сути множественное наследование, поэтому ManualLazy
    private ValueExpr valueExpr;
    @ManualLazy
    public ValueExpr getExpr() {
        if(valueExpr==null)
            valueExpr = new ValueExpr(this);
        return valueExpr;
    }
    public DataObject(ValueExpr valueExpr) {
        this(valueExpr.object, valueExpr.objectClass);
        this.valueExpr = valueExpr;
    }

    public InconsistentStaticValueExpr getInconsistentExpr() {
        return new InconsistentStaticValueExpr((ConcreteObjectClass) objectClass, object);
    }

    public Expr getStaticExpr() {
        assert !(objectClass instanceof StringClass);
        return new StaticValueExpr(object, (StaticClass) objectClass);
    }

    public Object getValue() {
        return object;
    }

    @Override
    public DataObject getObjectValue(ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) {
        return this;
    }

    public static <K> ImMap<K, DataObject> filterDataObjects(ImMap<K, ? extends ObjectValue> map) {
        return BaseUtils.immutableCast(
                ((ImMap<K, ObjectValue>)map).filterFnValues(new SFunctionSet<ObjectValue>() {
                    public boolean contains(ObjectValue element) {
                        return element instanceof DataObject;
                    }
                }));
    }

    public static <K, O extends ObjectValue> ImMap<K, DataObject> splitDataObjects(ImMap<K, O> map, Result<ImSet<K>> rNulls) {
        Result<ImMap<K, O>> rNullsMap = new Result<>();
        ImMap<K, O> result = map.splitKeys(new GetKeyValue<Boolean, K, O>() {
            @Override
            public Boolean getMapValue(K key, O value) {
                return value instanceof DataObject;
            }
        }, rNullsMap);
        rNulls.set(rNullsMap.result.keys());
        return BaseUtils.immutableCast(result);
    }

    public static <K> ImMap<K, DataObject> onlyDataObjects(ImMap<K, ? extends ObjectValue> map) {
        for(int i=0,size=map.size();i<size;i++)
            if(!(map.getValue(i) instanceof DataObject))
                return null;
        return BaseUtils.immutableCast(map);
    }

    public static <K> ImMap<K, DataObject> assertDataObjects(ImMap<K, ? extends ObjectValue> map) {
        assert onlyDataObjects(map) != null;
        return BaseUtils.immutableCast(map);
    }

    public static <K> ImMap<K,Object> getMapDataValues(ImMap<K, DataObject> map) {
        return map.mapValues(new GetValue<Object, DataObject>() {
            public Object getMapValue(DataObject value) {
                return value.object;
            }});
    }

    public static <K> ImMap<K,ConcreteClass> getMapDataClasses(ImMap<K, DataObject> map) {
        return map.mapValues(new GetValue<ConcreteClass, DataObject>() {
            public ConcreteClass getMapValue(DataObject value) {
                return value.objectClass;
            }});
    }

    public Where order(Expr expr, boolean desc, Where orderWhere) {
        if(orderWhere.isTrue()) // оптимизация для SQL серверов
            return expr.compare(this, desc ? Compare.LESS_EQUALS : Compare.GREATER_EQUALS);
        else {
            Where greater;
            if(desc)
                greater = expr.compare(this, Compare.GREATER_EQUALS).not();
            else
                greater = expr.compare(this,Compare.GREATER);
            return greater.or(expr.compare(this,Compare.EQUALS).and(orderWhere));
        }
    }

    public AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups) {
        return objectClass;
    }

    public DataObject getDataObject() {
        return this;
    }

    public ObjectValue getObjectValue() {
        return this;
    }

    public ConcreteClass getCurrentClass() {
        return objectClass;
    }

    public GroupObjectInstance getApplyObject() {
        return null;
    }

    public GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return null;
    }

    public Type getType() {
        return objectClass.getType();
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return this;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return getExpr().equals(((DataObject) o).getExpr());
    }

    protected int hash(HashValues hashValues) {
        return hashValues.hash(getExpr());
    }

    public ImSet<Value> getValues() {
        return SetFact.<Value>singleton(getExpr());
    }

    protected DataObject translate(MapValuesTranslate mapValues) {
        return mapValues.translate(getExpr()).getDataObject();
    }

    public DataObject refresh(SessionChanges session, ValueClass upClass) throws SQLException, SQLHandledException {
        return session.getDataObject(upClass, object);
    }

    public boolean isNull() {
        return false;
    }

    public ImSet<ObjectInstance> getObjectInstances() {
        return SetFact.EMPTY();
    }

    public PropertyObjectInterfaceInstance getRemappedInstance(ObjectEntity oldObject, ObjectInstance newObject, InstanceFactory instanceFactory) {
        return this;
    }

    public DataObject getDataObject(ImportTable.Row row) {
        return getDataObject();
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys) {
        return getExpr();
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) {
        return getExpr(importKeys);
    }

    public <K> ClassWhere<K> getClassWhere(K key) {
        return new ClassWhere<>(key, objectClass);
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) {
        return getExpr();
    }

    @Override
    public ConcreteClass getAndClassSet() {
        return objectClass;
    }

    public ParseInterface getParse(Type type, SQLSyntax syntax) {
        return new TypeObject(this, type, syntax);
    }

    @Override
    public String getShortName() {
        String result = object.toString();
        
        String shortName = objectClass.getShortName();
        if(!shortName.isEmpty())
            result += "-" + shortName;
        return result;
    }

    @Override
    public Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) {
        return getExpr();
    }
}
