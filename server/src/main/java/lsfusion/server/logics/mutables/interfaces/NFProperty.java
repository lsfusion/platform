package lsfusion.server.logics.mutables.interfaces;

import lsfusion.server.logics.mutables.Version;

public interface NFProperty<V> {
    
    void set(V value, Version version);
    
    V getNF(Version version);
    V get();
    
    V getDefault(NFDefault<V> def);
}
