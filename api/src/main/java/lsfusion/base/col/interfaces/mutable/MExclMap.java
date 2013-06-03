package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImMap;

public interface MExclMap<K, V> {
    
    void exclAdd(K key, V value);
    void exclAddAll(ImMap<? extends K, ? extends V> map);
    boolean isEmpty();

    V get(K key);
    int size();

    ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
