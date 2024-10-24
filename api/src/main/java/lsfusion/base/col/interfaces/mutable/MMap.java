package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MMap<K, V> {
    
    boolean add(K key, V value);
    boolean addAll(ImMap<? extends K, ? extends V> map);
    boolean addAll(ImSet<? extends K> set, V value);

    V get(K key);
    ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
