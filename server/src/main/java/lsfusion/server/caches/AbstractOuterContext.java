package lsfusion.server.caches;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.expr.UnionExpr;
import lsfusion.server.data.query.ExprEnumerator;
import lsfusion.server.data.translator.MapTranslate;

public abstract class AbstractOuterContext<T extends OuterContext<T>> extends AbstractKeysValuesContext<T> implements OuterContext<T> {

    public static ImSet<ParamExpr> getOuterKeys(OuterContext<?> context) {
        return getOuterSetKeys(context.getOuterDepends());
    }

    public static ImSet<Value> getOuterValues(OuterContext<?> context) {
        return getOuterColValues(context.getOuterDepends());
    }

    public static ImSet<StaticValueExpr> getOuterStaticValues(OuterContext<?> context) {
        return getOuterStaticValues(context.getOuterDepends());
    }

    public static long getComplexity(ImCol<? extends OuterContext> elements, boolean outer) {
        long complexity = 0;
        for(OuterContext element : elements)
            complexity += element.getComplexity(outer);
        return complexity;
    }

    public static ImSet<ParamExpr> getOuterColKeys(ImCol<? extends OuterContext> array) {
        MSet<ParamExpr> mResult = SetFact.mSet();
        for(OuterContext<?> element : array) {
            mResult.addAll(element.getOuterKeys());
        }
        return mResult.immutable();
    }

    public static ImSet<ParamExpr> getOuterSetKeys(ImSet<? extends OuterContext> array) {
        MSet<ParamExpr> mResult = SetFact.mSet();
        for(int i=0,size=array.size();i<size;i++)
            mResult.addAll(array.get(i).getOuterKeys());
        return mResult.immutable();
    }

    public static ImSet<Value> getOuterColValues(ImCol<? extends OuterContext> set) {
        MSet<Value> mResult = SetFact.mSet();
        for(int i=0,size=set.size();i<size;i++)
            mResult.addAll(set.get(i).getOuterValues());
        return mResult.immutable();
    }

    public static ImSet<StaticValueExpr> getOuterStaticValues(ImCol<? extends OuterContext> set) {
        MSet<StaticValueExpr> mResult = SetFact.mSet();
        for(int i=0,size=set.size();i<size;i++)
            mResult.addAll(set.get(i).getOuterStaticValues());
        return mResult.immutable();
    }

    public T translateOuter(MapTranslate translator) {
        return aspectTranslate(translator);
    }

    public int hashOuter(HashContext hashContext) {
        return aspectHash(hashContext);
    }

    public ImSet<Value> getOuterValues() {
        return aspectGetValues();
    }

    public ImSet<ParamExpr> getOuterKeys() {
        return aspectGetKeys();
    }

    // проверка на статичность, временно потом более сложный алгоритм надо будет
    public boolean isValue() {
        return getOuterKeys().isEmpty();
    }

    public int immutableHashCode() {
        return hashOuter(HashContext.hashCode);
    }

    public static int hashOuter(ImList<? extends OuterContext> list, HashContext hashContext) {
        int hash = 0;
        for(OuterContext element : list)
            hash = hash * 31 + element.hashOuter(hashContext);
        return hash;
    }

    public static int hashOuter(ImOrderMap<? extends OuterContext, ?> orders, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=orders.size();i<size;i++)
            hash = hash * 31 + orders.getKey(i).hashOuter(hashContext) ^ orders.getValue(i).hashCode();
        return hash;
    }

    public static int hashOuter(ImCol<? extends OuterContext> set, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=set.size();i<size;i++)
            hash += set.get(i).hashOuter(hashContext);
        return hash;
    }

    public static <T extends OuterContext> int hashSetOuter(T[] array, HashContext hashContext) {
        int hash = 0;
        for(OuterContext element : array)
            hash += element.hashOuter(hashContext);
        return hash;
    }

    public static int hashOuter(ImMap<?, ? extends OuterContext> map, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=map.size();i<size;i++)
            hash += map.getKey(i).hashCode() ^ map.getValue(i).hashOuter(hashContext);
        return hash;
    }

    public static int hashMapOuter(ImMap<? extends OuterContext, ? extends OuterContext> map, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=map.size();i<size;i++)
            hash += map.getKey(i).hashOuter(hashContext) ^ map.getValue(i).hashOuter(hashContext);
        return hash;
    }

    public static int hashKeysOuter(ImMap<? extends OuterContext, ?> map, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=map.size();i<size;i++)
            hash += map.getKey(i).hashOuter(hashContext) ^ map.getValue(i).hashCode();
        return hash;
    }

    public static Boolean enumerate(OuterContext<?> context, ExprEnumerator enumerator) {
        Boolean enumResult = enumerator.enumerate(context);
        if(enumResult!=null && enumResult) // идти внутрь
            for(OuterContext outerDepend : context.getOuterDepends())
                if(!outerDepend.enumerate(enumerator)) // выходим
                    return false;
        return enumResult!=null;
    }

    public boolean enumerate(ExprEnumerator enumerator) {
        return enumerate(this, enumerator);
    }

    public boolean hasUnionExpr() {
        final Result<Boolean> has = new Result<Boolean>(false);
        enumerate(new ExprEnumerator() {
            public Boolean enumerate(OuterContext join) {
                if(join instanceof UnionExpr) {
                    has.set(true);
                    return null;
                }
                return true;
            }
        });
        return has.result;
    }

    public static long getComplexity(OuterContext<?> context, boolean outer) {
        long result = 1;
        for(OuterContext outerDepend : context.getOuterDepends())
            result += outerDepend.getComplexity(outer);
        return result;
    }

    protected long calculateComplexity(boolean outer) {
        return getComplexity(this, outer);
    }

    protected abstract ImSet<OuterContext> calculateOuterDepends();
    private ImSet<OuterContext> outerDepends;
    public ImSet<OuterContext> getOuterDepends() {
        if(isComplex()) {
            if(outerDepends==null)
                outerDepends = calculateOuterDepends();
            return outerDepends;
        } else
            return calculateOuterDepends();
    }

    protected ImSet<ParamExpr> getKeys() {
        return getOuterKeys(this);
    }

    public ImSet<Value> getValues() {
        return getOuterValues(this);
    }

    public ImSet<StaticValueExpr> getOuterStaticValues() {
        return getOuterStaticValues(getOuterDepends());
    }
}
