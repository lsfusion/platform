package platform.server.data.expr;

import platform.server.data.query.HashContext;

import java.util.Map;

public abstract class GroupHashes {

    protected abstract int hashValue(HashContext hashContext);

    protected abstract Map<BaseExpr, BaseExpr> getGroup();

    // hash'и "внешнего" контекста, там пойдет внутренняя трансляция values поэтому hash по values надо "протолкнуть" внутрь
    public int hashContext(final HashContext hashContext) {
        HashContext innerHash = new HashContext() {
            public int hash(KeyExpr expr) {
                return 1;
            }

            public int hash(ValueExpr expr) {
                return hashContext.hash(expr);
            }
        };
        int hash = 0;
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : getGroup().entrySet())
            hash += groupExpr.getKey().hashContext(innerHash) ^ groupExpr.getValue().hashContext(hashContext);
        return hashValue(innerHash) * 31 + hash;
    }

    // hash'и "внутреннего" контекста
    public int hash(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<BaseExpr, BaseExpr> expr : getGroup().entrySet())
            hash += expr.getKey().hashContext(hashContext) ^ expr.getValue().hashCode();
        return hashValue(hashContext) * 31 + hash;
    }
}
