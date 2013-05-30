package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImSet;

public interface MFilterSet<K> {
    
    void keep(K element);
    
    ImSet<K> immutable();
}
