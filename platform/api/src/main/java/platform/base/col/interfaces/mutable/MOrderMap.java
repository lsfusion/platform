package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImOrderMap;

public interface MOrderMap<K, V> {
    
    void add(K key, V value);
    void addAll(ImOrderMap<? extends K, ? extends V> map);
    ImOrderMap<K, V> immutableOrder();
}
