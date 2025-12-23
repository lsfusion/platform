package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

public class NFAddFirstList<T> implements NFListChange<T> {

    private final T element;

    public NFAddFirstList(T element) {
        this.element = element;
    }

    public void proceedCol(MCol<T> mCol, Version version) {
    }

    public void proceedList(MList<T> list, Version version) {
        list.addFirst(element);
    }
}
