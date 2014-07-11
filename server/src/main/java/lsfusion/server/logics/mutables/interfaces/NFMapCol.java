package lsfusion.server.logics.mutables.interfaces;

import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NF;

public interface NFMapCol<K, V> extends NF {
    
    void addAll(K key, Iterable<V> it, Version version);
    void removeAll(K key, Version version);
    
}
