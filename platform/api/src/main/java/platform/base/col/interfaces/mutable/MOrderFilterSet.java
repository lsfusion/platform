package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderFilterSet<K> {
    
    void keep(K element);
    
    ImOrderSet<K> immutableOrder();
}
