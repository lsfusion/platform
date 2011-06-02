package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.GlobalObject;
import platform.base.GlobalInteger;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.KeyExpr;

import java.util.*;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public abstract class InnerHashContext extends ImmutableObject {

    public abstract int hashInner(HashContext hashContext);

    // строит hash с точностью до перестановок
    public int hashInner(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    private final Map<HashValues, BaseUtils.HashComponents<KeyExpr>> cacheComponents = new HashMap<HashValues, BaseUtils.HashComponents<KeyExpr>>();
    @ManualLazy
    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        BaseUtils.HashComponents<KeyExpr> result = cacheComponents.get(hashValues);
        if(result==null) {
            result = BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, GlobalInteger>() {

                public Map<KeyExpr, GlobalInteger> getParams() {
                    return BaseUtils.toMap(getKeys(), keyClass);
                }

                public int hashParams(Map<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(map.size()>0?new HashMapKeys(map):HashCodeKeys.instance, hashValues));
                }
            });
            cacheComponents.put(hashValues, result);
        }

        return result;
    }

    public abstract Set<KeyExpr> getKeys();
}
