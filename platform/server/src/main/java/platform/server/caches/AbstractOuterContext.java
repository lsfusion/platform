package platform.server.caches;

import platform.base.OrderedMap;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.translator.MapTranslate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractOuterContext<T extends OuterContext<T>> extends AbstractKeysValuesContext<T> implements OuterContext<T> {

    public static QuickSet<KeyExpr> getOuterKeys(OuterContext<?> context) {
        return getOuterKeys(context.getOuterDepends());
    }

    public static QuickSet<Value> getOuterValues(OuterContext<?> context) {
        return getOuterValues(context.getOuterDepends());
    }

    public static long getComplexity(Iterable<? extends OuterContext> elements, boolean outer) {
        long complexity = 0;
        for(OuterContext element : elements)
            complexity += element.getComplexity(outer);
        return complexity;
    }

    public static QuickSet<KeyExpr> getOuterKeys(Collection<? extends OuterContext> array) {
        QuickSet<KeyExpr> result = new QuickSet<KeyExpr>();
        for(OuterContext<?> element : array)
            result.addAll(element.getOuterKeys());
        return result;
    }

    public static QuickSet<KeyExpr> getOuterKeys(QuickSet<? extends OuterContext> array) {
        QuickSet<KeyExpr> result = new QuickSet<KeyExpr>();
        for(int i=0;i<array.size;i++)
            result.addAll(array.get(i).getOuterKeys());
        return result;
    }

    public static QuickSet<Value> getOuterValues(Collection<? extends OuterContext> set) {
        QuickSet<Value> result = new QuickSet<Value>();
        for(OuterContext<?> element : set)
            result.addAll(element.getOuterValues());
        return result;
    }

    public static QuickSet<Value> getOuterValues(QuickSet<? extends OuterContext> set) {
        QuickSet<Value> result = new QuickSet<Value>();
        for(int i=0;i<set.size;i++)
            result.addAll(set.get(i).getOuterValues());
        return result;
    }

    public T translateOuter(MapTranslate translator) {
        return aspectTranslate(translator);
    }

    public int hashOuter(HashContext hashContext) {
        return aspectHash(hashContext);
    }

    public QuickSet<Value> getOuterValues() {
        return aspectGetValues();
    }

    public QuickSet<KeyExpr> getOuterKeys() {
        return aspectGetKeys();
    }

    // проверка на статичность, временно потом более сложный алгоритм надо будет
    public boolean isValue() {
        return getOuterKeys().isEmpty();
    }

    public int immutableHashCode() {
        return hashOuter(HashContext.hashCode);
    }

    public static int hashOuter(List<? extends OuterContext> list, HashContext hashContext) {
        int hash = 0;
        for(OuterContext element : list)
            hash = hash * 31 + element.hashOuter(hashContext);
        return hash;
    }

    public static int hashOuter(OrderedMap<? extends OuterContext, ?> orders, HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<? extends OuterContext, ?> order : orders.entrySet())
            hash = hash * 31 + order.getKey().hashOuter(hashContext) ^ order.getValue().hashCode();
        return hash;
    }

    public static int hashOuter(Collection<? extends OuterContext> set, HashContext hashContext) {
        int hash = 0;
        for(OuterContext element : set)
            hash += element.hashOuter(hashContext);
        return hash;
    }

    public static int hashOuter(QuickSet<? extends OuterContext> set, HashContext hashContext) {
        int hash = 0;
        for(int i=0;i<set.size;i++)
            hash += set.get(i).hashOuter(hashContext);
        return hash;
    }

    public static <T extends OuterContext> int hashSetOuter(T[] array, HashContext hashContext) {
        int hash = 0;
        for(OuterContext element : array)
            hash += element.hashOuter(hashContext);
        return hash;
    }

    public static int hashOuter(Map<?, ? extends OuterContext> map, HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<?, ? extends OuterContext> entry : map.entrySet())
            hash += entry.getKey().hashCode() ^ entry.getValue().hashOuter(hashContext);
        return hash;
    }

    public static int hashKeysOuter(Map<? extends OuterContext, ?> map, HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<? extends OuterContext, ?> entry : map.entrySet())
            hash += entry.getKey().hashOuter(hashContext) ^ entry.getValue().hashCode();
        return hash;
    }

    public static int hashMapOuter(Map<? extends OuterContext, ? extends OuterContext> map, HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<? extends OuterContext, ? extends OuterContext> entry : map.entrySet())
            hash += entry.getKey().hashOuter(hashContext) ^ entry.getValue().hashOuter(hashContext);
        return hash;
    }

    public static int hashKeysOuter(QuickMap<? extends OuterContext, ?> map, HashContext hashContext) {
        int hash = 0;
        for(int i=0;i<map.size;i++)
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

    public static long getComplexity(OuterContext<?> context, boolean outer) {
        long result = 1;
        for(OuterContext outerDepend : context.getOuterDepends())
            result += outerDepend.getComplexity(outer);
        return result;
    }

    protected long calculateComplexity(boolean outer) {
        return getComplexity(this, outer);
    }

    protected abstract QuickSet<OuterContext> calculateOuterDepends();
    private QuickSet<OuterContext> outerDepends;
    @Override
    public QuickSet<OuterContext> getOuterDepends() {
        if(isComplex()) {
            if(outerDepends==null)
                outerDepends = calculateOuterDepends();
            return outerDepends;
        } else
            return calculateOuterDepends();
    }

    protected QuickSet<KeyExpr> getKeys() {
        return getOuterKeys(this);
    }

    public QuickSet<Value> getValues() {
        return getOuterValues(this);
    }
}
