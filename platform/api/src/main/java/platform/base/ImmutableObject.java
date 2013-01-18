package platform.base;

import platform.base.col.MapFact;
import platform.base.col.interfaces.mutable.add.MAddExclMap;

public abstract class ImmutableObject {
    
    private MAddExclMap caches = null;
    public MAddExclMap getCaches() {
        if(caches==null)
            caches = MapFact.mSmallCacheMap();
        return caches;
    }

}
