package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImOrderMap;

public interface MOrderExclMap<K, V> {

    void exclAdd(K key, V value);
    void exclAddAll(ImOrderMap<? extends K, ? extends V> map);
    ImOrderMap<K, V> immutableOrder();

    V get(K key);
}
