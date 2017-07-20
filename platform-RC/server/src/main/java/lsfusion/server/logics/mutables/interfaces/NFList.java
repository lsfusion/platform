package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.mutables.FindIndex;
import lsfusion.server.logics.mutables.Version;

public interface NFList<T> extends NFCol<T> {
    
    ImList<T> getNFList(Version version);
    Iterable<T> getNFListIt(Version version);
    Iterable<T> getListIt();
    ImList<T> getList();

    void addFirst(T element, Version version);
    
    void removeAll(Version version);
}
