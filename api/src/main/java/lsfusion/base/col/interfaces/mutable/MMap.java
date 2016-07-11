package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImMap;

public interface MMap<K, V> {
    
    boolean add(K key, V value);
    boolean addAll(ImMap<? extends K, ? extends V> map);

    V get(K key);
    ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
