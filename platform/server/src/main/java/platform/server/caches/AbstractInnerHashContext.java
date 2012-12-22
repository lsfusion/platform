package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GlobalInteger;
import platform.base.GlobalObject;
import platform.base.ImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.KeyExpr;

public abstract class AbstractInnerHashContext extends ImmutableObject implements InnerHashContext {

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, GlobalInteger>() {

                public ImMap<KeyExpr, GlobalInteger> getParams() {
                    return getInnerKeys().toMap(keyClass);
                }

                public int hashParams(ImMap<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(map.size()>0?new HashMapKeys(map): HashCodeKeys.instance, hashValues));
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
