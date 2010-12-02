package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.server.caches.hash.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.*;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public abstract class InnerHashContext extends ImmutableObject {

    public abstract int hashInner(HashContext hashContext);

    // строит hash с точностью до перестановок
    public int hashInner(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    private final Map<HashValues, BaseUtils.HashComponents<KeyExpr>> cacheComponents = new HashMap<HashValues, BaseUtils.HashComponents<KeyExpr>>();
    @ManualLazy
    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        BaseUtils.HashComponents<KeyExpr> result = cacheComponents.get(hashValues);
        if(result==null) {
            result = BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, Integer>() {

                public SortedMap<Integer, Set<KeyExpr>> getParams() {
                    TreeMap<Integer, Set<KeyExpr>> result = new TreeMap<Integer, Set<KeyExpr>>();
                    result.put(39916801, getKeys());
                    return result;
                }

                public int hashParamClass(KeyExpr param) {
                    return 1;
                }

                public int hashParams(Map<KeyExpr, Integer> map) {
                    return hashInner(new HashContext(map.size()>0?new HashMapKeys(map):HashCodeKeys.instance, hashValues));
                }
            });
            cacheComponents.put(hashValues, result);
        }

        return result;
    }

    public abstract Set<KeyExpr> getKeys();
}
