package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.impl.NF;
import lsfusion.server.base.version.Version;

public interface NFProperty<V> extends NF {
    
    void set(V value, Version version);
    
    V getNF(Version version);
    V get();
    
    V getDefault(NFDefault<V> def);
}
