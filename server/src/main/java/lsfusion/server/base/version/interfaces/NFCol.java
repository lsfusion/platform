package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

public interface NFCol<T> extends NF {
    
    void finalizeCol();
    
    void add(T element, Version version);
    
    Iterable<T> getNFIt(Version version);
    Iterable<T> getIt();
}
