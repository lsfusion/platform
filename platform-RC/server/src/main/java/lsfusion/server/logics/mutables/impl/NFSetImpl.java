package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.changes.NFSetChange;

public class NFSetImpl<T> extends NFASetImpl<T, NFSetChange<T>, ImSet<T>> {

    public NFSetImpl() {
    }

    public NFSetImpl(ImSet<T> changes) {
        super(changes);
    }

    public ImSet<T> getNF(Version version) {
        return getNFSet(version);
    }

    public ImSet<T> getSet() {
        return getFinal();
    }

    protected ImSet<T> getFinalSet(ImSet<T> fcol) {
        return fcol;
    }
}
