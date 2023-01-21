package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;

public interface NFOrderSet<T> extends NFSet<T>, NFList<T> {

    default void addFirst(T element, Version version) {
        throw new UnsupportedOperationException();
    }

    int size(Version version);

    ImOrderSet<T> getNFOrderSet(Version version);

    ImOrderSet<T> getOrderSet();
}
