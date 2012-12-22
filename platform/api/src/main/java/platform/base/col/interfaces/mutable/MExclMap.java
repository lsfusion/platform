package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;

public interface MExclMap<K, V> {
    
    void exclAdd(K key, V value);
    void exclAddAll(ImMap<? extends K, ? extends V> map);
    boolean isEmpty();

    V get(K key);

    ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
