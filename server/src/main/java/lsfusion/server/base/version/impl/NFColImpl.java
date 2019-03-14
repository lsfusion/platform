package lsfusion.server.base.version.impl;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFAdd;

public class NFColImpl<T> extends NFAColImpl<T, NFAdd<T>, ImCol<T>> {

    public NFColImpl() {
    }

    public NFColImpl(ImCol<T> changes) {
        super(changes);
    }

    public ImCol<T> getNF(Version version) {
        return getNFCol(version);
    }

    protected ImCol<T> getFinalCol(ImCol<T> fcol) {
        return fcol;
    }
}
