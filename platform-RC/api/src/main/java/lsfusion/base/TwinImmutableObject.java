package lsfusion.base;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWWVSMap;

public abstract class TwinImmutableObject<T extends TwinImmutableObject> extends ImmutableObject {

    protected abstract boolean calcTwins(TwinImmutableObject o);
    
    private static LRUWWVSMap<TwinImmutableObject, TwinImmutableObject, Boolean> cacheTwins = new LRUWWVSMap<>(LRUUtil.G1);    
    protected boolean twins(TwinImmutableObject o) {
        TwinImmutableObject c1 = this;
        TwinImmutableObject c2 = o; 
        if(System.identityHashCode(c1) > System.identityHashCode(c2)) {
            c1 = o; c2 = this;        
        }        
        Boolean result = cacheTwins.get(c1, c2);
        if(result == null) {
            result = calcTwins(o);
            cacheTwins.put(c1, c2, result);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((TwinImmutableObject) obj);
//        if(this == obj)
//            return true;
//
//        if(obj instanceof ImmutableObject)
//            return getClass() == obj.getClass() && twins((TwinImmutableObject) obj);
//
//        return equalAsCol(obj);
    }

    public abstract int immutableHashCode();

    // аналог AColObject, чтобы не тянуть caches из ImmutableObject
    private int hashCode;
    @Override
    public int hashCode() {
        if(hashCode==0) {
            hashCode = MapFact.objHash(immutableHashCode());
            if(hashCode==0)
                hashCode = Integer.MAX_VALUE;
        }
        return hashCode;
    }
}
