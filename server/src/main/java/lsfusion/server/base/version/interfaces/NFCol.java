package lsfusion.server.base.version.interfaces;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

import java.util.function.Function;

public interface NFCol<T> extends NF {
    
    void finalizeCol();
    
    void add(T element, Version version);

    void add(NFCol<T> element, Function<T, T> mapper, Version version);
    
    Iterable<T> getNFCopyIt(Version version);
    Iterable<T> getNFIt(Version version);
    Iterable<T> getIt();
}
