package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MExclMap<K, V> {
    
    void exclAdd(K key, V value);
    void exclAddAll(ImMap<? extends K, ? extends V> map);
    void exclAddAll(ImSet<? extends K> set, V value);
    boolean isEmpty();

    V get(K key);
    Iterable<K> keyIt();
    int size();

    ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
