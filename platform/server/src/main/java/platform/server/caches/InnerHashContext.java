package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.ImmutableObject;
import platform.server.caches.hash.*;
import platform.server.data.expr.KeyExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public abstract class InnerHashContext extends ImmutableObject {

    public abstract int hashInner(HashContext hashContext);

    // строит hash с точностью до перестановок
    public int hashInner(HashValues hashValues) {
        return getComponents(hashValues).hash;
    }

    public static HashValues getHashValues(boolean values) {
        return values?HashMapValues.instance:HashCodeValues.instance; 
    }

    public int hashInner(boolean values) {
        return hashInner(getHashValues(values));
    }

    // есть в общем то другая схема с генерацией всех перестановок, и поиском минимума (или суммированием)
    protected static class Components {
        protected Map<KeyExpr, Integer> map;
        protected int hash;

        public Components(Map<KeyExpr, Integer> map, int hash) {
            this.map = map;
            this.hash = hash;
        }
    }
    private final Map<HashValues, Components> cacheComponents = new HashMap<HashValues, Components>();
    @ManualLazy
    public Components getComponents(HashValues hashValues) {
        Components result = cacheComponents.get(hashValues);
        if(result==null) {
            Set<KeyExpr> freeKeys = getKeys();

            if(freeKeys.size()==0)
                return new Components(new HashMap<KeyExpr, Integer>(), hashInner(new HashContext(HashCodeKeys.instance, hashValues)));

            Map<KeyExpr, Integer> components = new HashMap<KeyExpr, Integer>();

            int resultHash = 0; // как по сути "список" минимальных хэшей
            int compHash = 16769023;
            while(freeKeys.size() > 0) {
                int minHash = Integer.MAX_VALUE;
                Set<KeyExpr> minKeys = new HashSet<KeyExpr>();
                for(KeyExpr key : freeKeys) {
                    Map<KeyExpr, Integer> mergedComponents = new HashMap<KeyExpr, Integer>(components); // необработанные в 0, обработанные в закинуты
                    mergedComponents.put(key, compHash);
                    for(KeyExpr mergeKey : freeKeys)
                        if(!key.equals(mergeKey))
                            mergedComponents.put(mergeKey, 39916801);

                    int hash = hashInner(new HashContext(new HashMapKeys(mergedComponents),hashValues));
                    if(hash < minHash) { // сбрасываем минимальные ключи
                        minKeys = new HashSet<KeyExpr>();
                        minHash = hash;
                    }

                    if(hash == minHash) // добавляем в минимальные ключи
                        minKeys.add(key);
                }

                for(KeyExpr key : minKeys)
                    components.put(key, compHash);

                resultHash = resultHash * 31 + minHash;

                freeKeys = BaseUtils.removeSet(freeKeys, minKeys);

                compHash = QuickMap.hash(compHash * 57793 + 9369319);
            }

            result = new Components(components, resultHash);
            cacheComponents.put(hashValues, result);
        }

        return result;
    }

    public abstract Set<KeyExpr> getKeys();
}
