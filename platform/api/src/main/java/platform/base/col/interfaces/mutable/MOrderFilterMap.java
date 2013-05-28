package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImOrderMap;

public interface MOrderFilterMap<K, V> {
    
    void keep(K key, V value);
    
    ImOrderMap<K, V> immutableOrder();
}
