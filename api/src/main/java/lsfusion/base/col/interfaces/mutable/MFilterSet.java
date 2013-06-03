package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MFilterSet<K> {
    
    void keep(K element);
    
    ImSet<K> immutable();
}
