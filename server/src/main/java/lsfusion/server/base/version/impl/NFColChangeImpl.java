package lsfusion.server.base.version.impl;

import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFAdd;
import lsfusion.server.base.version.interfaces.NFCol;

public abstract class NFColChangeImpl<T, CH, F extends Iterable<T>> extends NFChangeImpl<CH, F> implements NFCol<T> {

    public NFColChangeImpl() {
    }

    protected NFColChangeImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    public NFColChangeImpl(F changes) {
        super(changes);
    }

    public void finalizeCol() {
        getFinal();
    }

    public Iterable<T> getIt() {
        return getFinal();
    }

    protected boolean checkFinal(Object object) {
        return object instanceof Iterable;
    }

    public void add(T element, Version version) {
        addChange((CH) new NFAdd<>(element), version);
    }
}
