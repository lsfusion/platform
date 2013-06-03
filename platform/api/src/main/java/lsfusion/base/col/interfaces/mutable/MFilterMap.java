package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImMap;

public interface MFilterMap<K, V> {
    
    void keep(K key, V value);
    
    ImMap<K, V> immutable();
}
