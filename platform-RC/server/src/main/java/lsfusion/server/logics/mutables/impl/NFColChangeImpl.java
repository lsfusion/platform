package lsfusion.server.logics.mutables.impl;

import lsfusion.server.logics.mutables.interfaces.NFCol;

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
