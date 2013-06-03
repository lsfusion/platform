package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.MapFact;

public abstract class AColObject {

    public abstract int immutableHashCode();

    // аналог TwinImmutableObject, чтобы не тянуть caches из ImmutableObject
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
