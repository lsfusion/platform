package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.GlobalObject;
import platform.base.GlobalInteger;
import platform.server.caches.hash.*;
import platform.server.data.expr.KeyExpr;

import java.util.*;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public abstract class InnerHashContext extends ImmutableObject {

    public abstract int hashInner(HashContext hashContext);

    public abstract Set<KeyExpr> getKeys();

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    @ManualLazy
    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, GlobalInteger>() {

                public Map<KeyExpr, GlobalInteger> getParams() {
                    return BaseUtils.toMap(getKeys(), keyClass);
                }

                public int hashParams(Map<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(map.size()>0?new HashMapKeys(map):HashCodeKeys.instance, hashValues));
                }
            });
    }
}
