package platform.base;

import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;

public abstract class ImmutableObject {
    
    private MCacheMap caches = null;
    public MCacheMap getCaches() {
        if(caches==null)
            caches = LRUCache.mSmall(LRUCache.EXP_RARE);
        return caches;
    }

}
