package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.server.logics.mutables.FindIndex;
import lsfusion.server.logics.mutables.Version;

import java.util.Collection;

public interface NFCol<T> {
    
    void finalizeCol();
    
    void add(T element, Version version);
    
    Iterable<T> getNFIt(Version version);
    Iterable<T> getIt();
}
