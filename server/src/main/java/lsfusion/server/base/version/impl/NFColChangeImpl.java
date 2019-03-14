package lsfusion.server.base.version.impl;

import lsfusion.server.base.version.interfaces.NFCol;

public abstract class NFColChangeImpl<T, CH, F extends Iterable<T>> extends NFChangeImpl<T, CH, F> implements NFCol<T> {

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
}
