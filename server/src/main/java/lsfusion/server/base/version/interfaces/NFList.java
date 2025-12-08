package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.version.Version;

import java.util.function.Function;
import java.util.function.Predicate;

public interface NFList<T> extends NFCol<T> {

    void add(NFList<T> element, Function<T, T> mapper, Version version);

    ImList<T> getNFCopyList(Version version);
    ImList<T> getNFList(Version version);
    Iterable<T> getNFListIt(Version version);
    Iterable<T> getListIt();
    ImList<T> getList();

    void addFirst(T element, Version version);
    
    void removeAll(Predicate<T> filter, Version version);
}
