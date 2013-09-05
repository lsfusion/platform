package lsfusion.base;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;

public abstract class TwinImmutableObject<T extends TwinImmutableObject> extends ImmutableObject {

    public abstract boolean twins(TwinImmutableObject o);

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
