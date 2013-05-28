package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImRevMap;

public interface MFilterRevMap<K, V> {

    void revKeep(K key, V value);
    
    ImRevMap<K, V> immutableRev();
}
