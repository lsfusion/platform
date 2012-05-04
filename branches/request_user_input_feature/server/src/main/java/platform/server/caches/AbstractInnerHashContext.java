package platform.server.caches;

import platform.base.*;
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

                public QuickMap<KeyExpr, GlobalInteger> getParams() {
                    return getInnerKeys().toQuickMap(keyClass);
                }

                public int hashParams(QuickMap<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(map.size>0?new HashMapKeys(map): HashCodeKeys.instance, hashValues));
                }
            });
    }
}
