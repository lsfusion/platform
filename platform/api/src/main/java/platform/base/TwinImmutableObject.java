package platform.base;

import platform.base.col.MapFact;

public abstract class TwinImmutableObject extends ImmutableObject {

    public abstract boolean twins(TwinImmutableObject o);

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((TwinImmutableObject) obj);
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
