package lsfusion.server.data.expr;

import lsfusion.base.GlobalObject;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.*;
import lsfusion.server.data.Value;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

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

    public static IntegralClass COUNTCLASS = IntegerClass.instance;
    public static StaticValueExpr COUNT = new StaticValueExpr(1, COUNTCLASS);

    public String getSource(CompileSource compile) {
        return compile.params.get(this);
    }

    public Type getType(KeyType keyType) {
        return getType();
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public boolean calcTwins(TwinImmutableObject o) {
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
    public static boolean noStaticContains(ImSet<? extends Value> col1, ImSet<? extends Value> col2) {
        return ((ImSet<Value>)removeStatic(col1)).containsAll(removeStatic(col2));
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
