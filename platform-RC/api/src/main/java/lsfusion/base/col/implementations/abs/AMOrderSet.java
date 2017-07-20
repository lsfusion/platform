package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderFilterSet;
import lsfusion.base.col.interfaces.mutable.MOrderSet;

public abstract class AMOrderSet<K> extends AOrderSet<K> implements MOrderSet<K>, MOrderExclSet<K>, MOrderFilterSet<K> {

    public void addAll(ImOrderSet<? extends K> ks) {
        for(int i=0,size=ks.size();i<size;i++)
            add(ks.get(i));
    }

    public void exclAddAll(ImOrderSet<? extends K> ks) {
        for(int i=0,size=ks.size();i<size;i++)
            exclAdd(ks.get(i));
    }

    public void keep(K element) {
        exclAdd(element);
    }

    public ImOrderSet<K> immutableOrder() {
        return this;
    }
}
