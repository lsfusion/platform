package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.mutables.Version;

import java.util.Set;

public interface NFSet<T> extends NFCol<T> {
    
    ImSet<T> getNFSet(Version version);
    boolean containsNF(T element, Version version);
    ImSet<T> getSet();

    void remove(T element, Version version);

}
