package lsfusion.server.logics.mutables.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;

public class NFAddFirstList<T> implements NFListChange<T> {

    private final T element;

    public NFAddFirstList(T element) {
        this.element = element;
    }

    public void proceedCol(MCol<T> mCol) {
    }

    public void proceedList(MList<T> list) {
        list.addFirst(element);
    }
}
