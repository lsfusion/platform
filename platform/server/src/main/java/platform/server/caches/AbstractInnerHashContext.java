package platform.server.caches;

import platform.base.*;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.KeyExpr;

public abstract class AbstractInnerHashContext extends AbstractHashContext<HashValues> implements InnerHashContext {

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues) {
        return aspectHash(hashValues);
    }

    protected int hash(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    public boolean twins(TwinImmutableObject o) {
        throw new UnsupportedOperationException();
    }

    public int immutableHashCode() {
        throw new UnsupportedOperationException();
    }

    protected HashValues aspectContextHash(HashValues hash) {
        return hash.filterValues(getInnerValues());
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, GlobalInteger>() {

                public ImMap<KeyExpr, GlobalInteger> getParams() {
                    return getInnerKeys().toMap(keyClass);
                }

                public int hashParams(ImMap<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(HashContext.create(HashMapKeys.create(map), hashValues));
                }
            });
    }

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
