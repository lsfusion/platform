package platform.server.caches;

import platform.base.ImmutableObject;
import platform.base.OrderedMap;
import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.SourceJoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractOuterContext<This extends OuterContext> extends TwinImmutableObject implements OuterContext<This> {

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

    public static Set<Value> getOuterValues(OuterContext outerContext) {
        return AbstractSourceJoin.enumValues(outerContext.getEnum());
    }

    @IdentityLazy
    public Set<Value> getOuterValues() {
        return getOuterValues(this);
    }
}
