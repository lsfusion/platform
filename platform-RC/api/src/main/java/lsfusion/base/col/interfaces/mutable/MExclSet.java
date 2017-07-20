package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MExclSet<K> {
    
    void exclAdd(K key);
    void exclAddAll(ImSet<? extends K> set);

    ImSet<K> immutable();
    Iterable<K> it();

    // для MCaseList
    int size();
    K single();
}
