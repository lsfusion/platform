package lsfusion.server.base.version.interfaces;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;


// order set with insert types and group
public interface NFComplexOrderSet<T> extends NF {

    void add(T element, ComplexLocation<T> location, Version version);

    void remove(T element, Version version);

    int size(Version version);

    ImOrderSet<T> getOrderSet();

    Iterable<T> getNFIt(Version version);
    Iterable<T> getIt();

    Pair<ImOrderSet<T>, ImList<Integer>> getNFComplexOrderSet(Version version);
    ImList<T> getNFList(Version version);
    Iterable<T> getNFListIt(Version version);
    Iterable<T> getListIt();
    ImList<T> getList();
    ImSet<T> getSet();
}
