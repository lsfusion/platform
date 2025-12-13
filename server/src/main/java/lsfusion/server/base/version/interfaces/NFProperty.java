package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;
import lsfusion.server.base.version.impl.changes.NFCopy;

public interface NFProperty<V> extends NF {
    
    void set(V value, Version version);
    void set(NFProperty<V> value, NFCopy.Map<V> mapping, Version version);

    V getNF(Version version);
    V get();
}
