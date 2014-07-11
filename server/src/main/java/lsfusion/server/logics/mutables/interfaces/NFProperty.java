package lsfusion.server.logics.mutables.interfaces;

import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NF;

public interface NFProperty<V> extends NF {
    
    void set(V value, Version version);
    
    V getNF(Version version);
    V get();
    
    V getDefault(NFDefault<V> def);
}
