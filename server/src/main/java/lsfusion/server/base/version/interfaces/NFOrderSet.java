package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFCopy;

public interface NFOrderSet<T> extends NFSet<T>, NFList<T> {

    void add(NFOrderSet<T> element, NFCopy.Map<T> mapper, Version version);

    default void addFirst(T element, Version version) {
        throw new UnsupportedOperationException();
    }

    int size(Version version);

    ImOrderSet<T> getNFCopyOrderSet(Version version);
    ImOrderSet<T> getNFOrderSet(Version version);

    ImOrderSet<T> getOrderSet();
}
