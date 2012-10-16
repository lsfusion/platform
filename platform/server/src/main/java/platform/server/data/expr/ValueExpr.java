package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.GlobalObject;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.*;
import platform.server.data.Value;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.type.TypeObject;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ValueExpr extends StaticExpr<ConcreteClass> implements Value {

    public final Object object;

    public Value removeBig(QuickSet<Value> usedValues) {
        if(objectClass instanceof FileClass && ((byte[])object).length > 1000) {
            int i=0;
            while(true) {
                Value removeValue = new ValueExpr(new BigInteger(""+i).toByteArray(), objectClass);
                if(!usedValues.contains(removeValue))
                    return removeValue;
            }
        }
        return null;
    }

    public ValueExpr(Object object, ConcreteClass objectClass) {
        super(objectClass);

        this.object = object;
    }

    public static StaticValueExpr TRUE = new StaticValueExpr(true,LogicalClass.instance);
    public static Expr get(Where where) {
        return TRUE.and(where);
    }

    public static StaticValueExpr COUNT = new StaticValueExpr(1, IntegerClass.instance);

    public String getSource(CompileSource compile) {
        return compile.params.get(this);
    }

    public Type getType() {
        return objectClass.getType();
    }

    public Type getType(KeyType keyType) {
        return getType();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public boolean twins(TwinImmutableInterface o) {
        return object.equals(((ValueExpr)o).object) && objectClass.equals(((ValueExpr)o).objectClass);
    }

    @Override
    public int immutableHashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    protected int hash(HashContext hashContext) {
        return hashContext.values.hash(this);
    }

    // нельзя потому как при трансляции значения потеряются
/*    @Override
    public ValueExpr scale(int mult) {
        return new ValueExpr(((IntegralClass)objectClass).multiply((Number) object,mult),objectClass);
    }*/

    public ValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    protected ValueExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }

    public QuickSet<Value> getValues() {
        return new QuickSet<Value>(this);
    }

    public static Value ZERO = new ValueExpr(0.0, DoubleClass.instance);
    public static Value TRUEVAL = new ValueExpr(true, LogicalClass.instance);

    private static Set<Value> staticExprs;
    private static Set<Value> getStaticExprs() {
        if(staticExprs == null) {
            staticExprs = new HashSet<Value>();
            staticExprs.add(ValueExpr.ZERO);
            staticExprs.add(ValueExpr.TRUEVAL);
            staticExprs.add(ActionClass.instance.getDefaultExpr());
            staticExprs.add(null);
        }
        return staticExprs;
    }

    public static Set<? extends Value> removeStatic(Set<? extends Value> col) {
        Set<Value> result = new HashSet<Value>();
        for(Value value : BaseUtils.removeSet(col,getStaticExprs()))
            if(!(value instanceof ValueExpr && ((ValueExpr)value).objectClass instanceof ActionClass)) // && ((ValueExpr) value).equals(((ActionClass)((ValueExpr)value).objectClass).getDefaultExpr())))
                result.add(value);
        return result;
    }

    public static <V> Map<Value,V> removeStatic(Map<Value,V> map) {
        return BaseUtils.filterNotKeys(map,getStaticExprs());
    }

    // пересечение с игнорированием ValueExpr.TRUE
    public static boolean noStaticEquals(Set<? extends Value> col1, Set<? extends Value> col2) {
        return removeStatic(col1).equals(removeStatic(col2));
    }

    public TypeObject getParseInterface() {
        return new TypeObject(object, objectClass.getType());
    }

    public GlobalObject getValueClass() {
        return objectClass;
    }

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    private DataObject dataObject;
    @ManualLazy
    public DataObject getDataObject() { // по сути множественное наследование, поэтому ManualLazy
        if(dataObject==null)
            dataObject = new DataObject(this);
        return dataObject;
    }
    public ValueExpr(DataObject dataObject) {
        this(dataObject.object, dataObject.objectClass);
    }

    @Override
    public ObjectValue getObjectValue() {
        return getDataObject();
    }

    @Override
    public boolean compatibleEquals(BaseExpr expr) {
        return super.compatibleEquals(expr) || expr instanceof ValueExpr && objectClass instanceof DataClass && ((ValueExpr) expr).objectClass instanceof DataClass && ((DataClass) objectClass).compatibleEquals(object, (DataClass) ((ValueExpr) expr).objectClass, ((ValueExpr) expr).object);
    }
}
