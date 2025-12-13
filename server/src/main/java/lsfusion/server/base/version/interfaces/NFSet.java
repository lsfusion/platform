package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFCopy;

public interface NFSet<T> extends NFCol<T> {

    void add(NFSet<T> element, NFCopy.Map<T> mapper, Version version);

    ImSet<T> getNFCopySet(Version version);
    boolean containsNF(T element, Version version);
    ImSet<T> getSet();

    void remove(T element, Version version);

}
