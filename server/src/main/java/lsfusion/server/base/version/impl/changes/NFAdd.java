package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

import java.util.List;
import java.util.Set;

public class NFAdd<T> implements NFListChange<T>, NFOrderSetChange<T> {
    public final T element;

    public NFAdd(T element) {
        this.element = element;
    }

    public void proceedCol(MCol<T> mCol, Version version) {
        mCol.add(element);
    }
    
    public void proceedSet(Set<T> mSet, Version version) {
        mSet.add(element);
    }

    public void proceedList(MList<T> list, Version version) {
        list.add(element);
    }

    public void proceedOrderSet(List<T> list, Version version) {
        if (!list.contains(element)) {
            list.add(element);
        }
    }
}
