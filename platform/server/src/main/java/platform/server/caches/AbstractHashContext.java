package platform.server.caches;

import platform.base.TwinImmutableObject;
import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;
import platform.server.Settings;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashObject;

public abstract class AbstractHashContext<H extends HashObject> extends TwinImmutableObject {

    protected boolean isComplex() { // замена аннотации
        return false;
    }

    // использование в AbstractHashContext
    protected abstract H aspectContextHash(H hash);

//    private Integer globalHash;
    private Object hashes;
    @ManualLazy
    protected int aspectHash(H hash) {
        if(isComplex()) {
            if(hash.isGlobal()) {
//                if(hash == HashCodeValues.instance || (hash instanceof HashContext && ((HashContext)hash).keys== HashCodeKeys.instance && ((HashContext)hash).values==HashCodeValues.instance))

                if(!Settings.get().isCacheInnerHashes()) {
                    // сделал isGlobal только HashCode*, так как getKeys, getValues и фильтрация достаточно много жрут
                    assert hash == HashCodeValues.instance || (hash instanceof HashContext && ((HashContext)hash).keys== HashCodeKeys.instance && ((HashContext)hash).values==HashCodeValues.instance);
                    if(hashes==null)
                        hashes = hash(hash);
                    return (Integer)hashes;
                } else {
                    MCacheMap<H, Integer> mapHashes = (MCacheMap<H, Integer>)hashes;
                    if(mapHashes==null) {
                        mapHashes = LRUCache.mSmall(LRUCache.EXP_QUICK);
                        hashes = mapHashes;
                    }

                    hash = aspectContextHash(hash);
                    Integer result;
                    synchronized (mapHashes) {
                        result = mapHashes.get(hash);
                        if(result==null) {
                            result = hash(hash);
                            mapHashes.exclAdd(hash, result);
                        }
                    }
                    return result;
                }
            } else {
                Integer cacheResult = hash.aspectGetCache(this);
                if(cacheResult==null) {
                    cacheResult = hash(hash);
                    hash.aspectSetCache(this, cacheResult);
                }
                return cacheResult;
            }
        } else
            return hash(hash);
    }
    protected abstract int hash(H hash); // по сути protected

}
