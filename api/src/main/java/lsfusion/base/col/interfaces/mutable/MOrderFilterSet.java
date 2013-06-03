package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderFilterSet<K> {
    
    void keep(K element);
    
    ImOrderSet<K> immutableOrder();
}
