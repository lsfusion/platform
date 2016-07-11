package lsfusion.server.caches;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.lru.*;
import lsfusion.server.Settings;
import lsfusion.server.caches.hash.HashCodeKeys;
import lsfusion.server.caches.hash.HashCodeValues;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.caches.hash.HashObject;

public abstract class AbstractHashContext<H extends HashObject> extends TwinImmutableObject {

    protected boolean isComplex() { // замена аннотации
        return false;
    }

    // использование в AbstractHashContext
    protected abstract H aspectContextHash(H hash);

//    private Integer globalHash;
    private Object hashes;
    
    private final static LRUWSVSMap<AbstractHashContext, HashObject, Integer> cacheHashes = new LRUWSVSMap<>(LRUUtil.L1);
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
                    hash = aspectContextHash(hash);

/*                    MCacheMap<H, Integer> mapHashes = (MCacheMap<H, Integer>)hashes;
                    if(mapHashes==null) {
                        mapHashes = LRUCache.mSmall(LRUCache.EXP_QUICK);
                        hashes = mapHashes;
                    }
                    Integer result;
                    synchronized (mapHashes) {
                        result = mapHashes.get(hash);
                        if(result==null) {
                            result = hash(hash);
                            mapHashes.exclAdd(hash, result);
                        }
                    }*/

                    Integer result = cacheHashes.get(this, hash);
                    if(result==null) {
                        result = hash(hash);
                        cacheHashes.put(this, hash, result);
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
