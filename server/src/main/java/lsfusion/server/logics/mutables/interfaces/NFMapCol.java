package lsfusion.server.logics.mutables.interfaces;

import lsfusion.server.logics.mutables.Version;

public interface NFMapCol<K, V> {
    
    void addAll(K key, Iterable<V> it, Version version);
    void removeAll(K key, Version version);
    
}
