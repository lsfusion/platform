package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImRevMap;

public interface MRevMap<K, V> {
    
    void revAdd(K key, V value);
    void revAddAll(ImRevMap<? extends K, ? extends V> map);

    V get(K key);
    boolean containsKey(K key);
    int size();

    ImRevMap<K, V> immutableRev();
}
