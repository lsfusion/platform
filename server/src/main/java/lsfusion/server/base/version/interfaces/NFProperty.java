package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

import java.util.function.Function;

public interface NFProperty<V> extends NF {
    
    void set(V value, Version version);
    void set(NFProperty<V> value, Function<V, V> mapping, Version version);

    V getNF(Version version);
    V get();
}
