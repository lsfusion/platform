package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.SFunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashValues;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.integration.*;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.Modifier;
import platform.server.session.SessionChanges;
import platform.server.session.SessionTableUsage;
import platform.server.session.SinglePropertyTableUsage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public class DataObject extends ObjectValue<DataObject> implements PropertyObjectInterfaceInstance, PropertyObjectInterfaceEntity, ImportKeyInterface, ImportFieldInterface, ImportDeleteInterface {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public DataObject() {

    }
    
    public DataObject(Object object, ConcreteClass objectClass) {
        if(objectClass instanceof StringClass)
            object = ((StringClass)objectClass).read(object);

        this.object = object;

        assert objectClass.getType().read(object).equals(object); // чтобы читалось то что писалось

        this.objectClass = objectClass;
    }

    public DataObject(String string) {
        this(string, StringClass.get(string.length()));
    }

    public DataObject(Double dbl) {
        this(dbl, DoubleClass.instance);
    }

    public DataObject(Boolean bl) {
        this(bl, LogicalClass.instance);
    }

    public DataObject(Integer ig) {
        this(ig, IntegerClass.instance);
    }

    public boolean isString(SQLSyntax syntax) {
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

    public Expr getStaticExpr() {
        return new StaticValueExpr(object, (StaticClass) objectClass);
    }

    public Object getValue() {
        return object;
    }

    public static <K> ImMap<K, DataObject> filterDataObjects(ImMap<K, ObjectValue> map) {
        return BaseUtils.immutableCast(
                map.filterFnValues(new SFunctionSet<ObjectValue>() {
                    public boolean contains(ObjectValue element) {
                        return element instanceof DataObject;
                    }
                }));
    }

    public static <K> ImMap<K,Object> getMapValues(ImMap<K,DataObject> map) {
        return map.mapValues(new GetValue<Object, DataObject>() {
            public Object getMapValue(DataObject value) {
                return value.object;
            }});
    }

    public static <K> ImMap<K,ConcreteClass> getMapClasses(ImMap<K,DataObject> map) {
        return map.mapValues(new GetValue<ConcreteClass, DataObject>() {
            public ConcreteClass getMapValue(DataObject value) {
                return value.objectClass;
            }});
    }

    public Where order(Expr expr, boolean desc, Where orderWhere) {
        if(orderWhere.isTrue()) // оптимизация для SQL серверов
            return expr.compare(this, desc ? Compare.LESS_EQUALS : Compare.GREATER_EQUALS);
        else {
            Where greater = expr.compare(this,Compare.GREATER);
            return (desc?greater.not():greater).or(expr.compare(this,Compare.EQUALS).and(orderWhere));
        }
    }

    public AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups) {
        return objectClass;
    }

    public DataObject getDataObject() {
        return this;
    }

    public ConcreteClass getCurrentClass() {
        return objectClass;
    }

    public GroupObjectInstance getApplyObject() {
        return null;
    }

    public Type getType() {
        return objectClass.getType();
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return this;
    }

    public void fillObjects(Set<ObjectEntity> objects) {
    }

    public boolean twins(TwinImmutableObject o) {
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

    public DataObject refresh(SessionChanges session, ValueClass upClass) throws SQLException {
        return session.getDataObject(upClass, object);
    }

    public boolean isNull() {
        return false;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        BaseUtils.serializeObject(outStream, object);

        TypeSerializer.serializeType(outStream, getType());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        object = BaseUtils.deserializeObject(inStream);

        objectClass = pool.context.BL.getDataClass(object, TypeSerializer.deserializeType(inStream, 9999999));
    }

    public ImSet<ObjectInstance> getObjectInstances() {
        return SetFact.EMPTY();
    }

    public DataObject getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
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
        return new ClassWhere<K>(key, objectClass);
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) {
        return getExpr();
    }
}
