package platform.base.col.implementations.abs;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.MOrderFilterSet;
import platform.base.col.interfaces.mutable.MOrderSet;

public abstract class AMOrderSet<K> extends AOrderSet<K> implements MOrderSet<K>, MOrderExclSet<K>, MOrderFilterSet<K> {

    public void addAll(ImOrderSet<? extends K> ks) {
        for(int i=0,size=ks.size();i<size;i++)
            add(ks.get(i));
    }

    public void keep(K element) {
        add(element);
    }

    public ImOrderSet<K> immutableOrder() {
        return this;
    }
}
