package platform.server.caches;

import org.w3c.css.sac.ElementSelector;
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

    // строит hash с точностью до перестановок
    public int hashInner(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    private final static GlobalInteger keyClass = new GlobalInteger(39916801);

    private BaseUtils.HashComponents<KeyExpr> calculateComponents(final HashValues hashValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<KeyExpr, GlobalInteger>() {

                public Map<KeyExpr, GlobalInteger> getParams() {
                    return BaseUtils.toMap(getKeys(), keyClass);
                }

                public int hashParams(Map<KeyExpr, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(map.size()>0?new HashMapKeys(map):HashCodeKeys.instance, hashValues));
                }
            });
    }

    private BaseUtils.HashComponents<KeyExpr> cacheComponents;
    private BaseUtils.HashComponents<KeyExpr> getCacheComponents(HashValues hashValues) {
        if(hashValues instanceof HashLocalValues) {
            HashLocalValues hashLocalValues = (HashLocalValues)hashValues;
            return hashLocalValues.getCacheComponents().get(this);
        } else {
            assert hashValues.equals(HashCodeValues.instance);
            return cacheComponents;
        }
    }
    private void setCacheComponents(HashValues hashValues, BaseUtils.HashComponents<KeyExpr> result) {
        if(hashValues instanceof HashLocalValues) {
            HashLocalValues hashLocalValues = (HashLocalValues)hashValues;
            hashLocalValues.getCacheComponents().put(this, result);
        } else {
            assert hashValues.equals(HashCodeValues.instance);
            cacheComponents = result;
        }
    }

    @ManualLazy
    public BaseUtils.HashComponents<KeyExpr> getComponents(final HashValues hashValues) {
        BaseUtils.HashComponents<KeyExpr> result = getCacheComponents(hashValues);
        if(result==null) {
            result = calculateComponents(hashValues);
            setCacheComponents(hashValues, result);
        }
        return result;
    }

    public abstract Set<KeyExpr> getKeys();
}
