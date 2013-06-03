package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;

public interface MOrderMap<K, V> {
    
    void add(K key, V value);
    void addAll(ImOrderMap<? extends K, ? extends V> map);
    ImOrderMap<K, V> immutableOrder();
}
