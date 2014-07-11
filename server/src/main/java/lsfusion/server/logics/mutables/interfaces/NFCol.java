package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.server.logics.mutables.FindIndex;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NF;

import java.util.Collection;

public interface NFCol<T> extends NF {
    
    void finalizeCol();
    
    void add(T element, Version version);
    
    Iterable<T> getNFIt(Version version);
    Iterable<T> getIt();
}
