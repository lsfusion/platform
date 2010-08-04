package platform.server.data.expr.query;

import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;

import java.util.Map;

public abstract class QueryHashes<K extends BaseExpr> {

    protected abstract int hashValue(HashContext hashContext);

    protected abstract Map<K, BaseExpr> getGroup();

    // hash'и "внешнего" контекста, там пойдет внутренняя трансляция values поэтому hash по values надо "протолкнуть" внутрь
    @IdentityLazy
    public int hashContext(final HashContext hashContext) {
        HashContext innerHash = hashContext.mapKeys();
        int hash = 0;
        for(Map.Entry<K,BaseExpr> groupExpr : getGroup().entrySet())
            hash += groupExpr.getKey().hashContext(innerHash) ^ groupExpr.getValue().hashContext(hashContext);
        return hashValue(innerHash) * 31 + hash;
    }

    // hash'и "внутреннего" контекста
    @IdentityLazy
    public int hash(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<K,BaseExpr> expr : getGroup().entrySet())
            hash += expr.getKey().hashContext(hashContext) ^ expr.getValue().hashCode();
        return hashValue(hashContext) * 31 + hash;
    }
}
