package platform.server.data.expr;

import platform.base.GlobalObject;
import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.*;
import platform.server.data.Value;
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


public class ValueExpr extends StaticExpr<ConcreteClass> implements Value {

    public final Object object;

    public Value removeBig(MAddSet<Value> usedValues) {
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

    public Type getType(KeyType keyType) {
        return getType();
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public boolean twins(TwinImmutableObject o) {
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

    public ImSet<Value> getValues() {
        return SetFact.<Value>singleton(this);
    }

    public static Value ZERO = new ValueExpr(0.0, DoubleClass.instance);
    public static Value TRUEVAL = new ValueExpr(true, LogicalClass.instance);

    private static ImSet<Value> staticExprs;
    private static ImSet<Value> getStaticExprs() {
        if(staticExprs == null) {
            MExclSet<Value> mStaticExprs = SetFact.mExclSet(4);
            mStaticExprs.exclAdd(ValueExpr.ZERO);
            mStaticExprs.exclAdd(ValueExpr.TRUEVAL);
            mStaticExprs.exclAdd(ActionClass.instance.getDefaultExpr());
            staticExprs = mStaticExprs.immutable();
        }
        return staticExprs;
    }

    public static ImSet<? extends Value> removeStatic(ImSet<? extends Value> col) {
        ImSet<Value> cleanCol = SetFact.remove(col, getStaticExprs());
        MExclSet<Value> mResult = SetFact.mExclSet(cleanCol.size());
        for(Value value : cleanCol)
            if(!(value instanceof ValueExpr && ((ValueExpr)value).objectClass instanceof ActionClass)) // && ((ValueExpr) value).equals(((ActionClass)((ValueExpr)value).objectClass).getDefaultExpr())))
                mResult.exclAdd(value);
        return mResult.immutable();
    }

    public static <V> ImMap<Value,V> removeStatic(ImMap<Value,V> map) {
        return map.remove(getStaticExprs());
    }

    // пересечение с игнорированием ValueExpr.TRUE
    public static boolean noStaticEquals(ImSet<? extends Value> col1, ImSet<? extends Value> col2) {
        return ((ImSet<Value>)removeStatic(col1)).equals(removeStatic(col2));
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
