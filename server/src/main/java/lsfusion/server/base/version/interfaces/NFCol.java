package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;
import lsfusion.server.base.version.impl.changes.NFCopy;

public interface NFCol<T> extends NF {
    
    void finalizeCol();
    
    void add(T element, Version version);

    void add(NFCol<T> element, NFCopy.Map<T> mapper, Version version);
    
    Iterable<T> getNFCopyIt(Version version);
    Iterable<T> getNFIt(Version version);
    Iterable<T> getNFIt(Version version, boolean allowRead);
    Iterable<T> getIt();
}
