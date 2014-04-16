package lsfusion.server.logics.mutables.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;

import java.util.List;
import java.util.Set;

public class NFAdd<T> implements NFListChange<T>, NFOrderSetChange<T> {
    public final T element;

    public NFAdd(T element) {
        this.element = element;
    }

    public void proceedCol(MCol<T> mCol) {
        mCol.add(element);
    }
    
    public void proceedSet(Set<T> mSet) {
        mSet.add(element);
    }

    public void proceedList(MList<T> list) {
        list.add(element);
    }

    public void proceedOrderSet(List<T> list) {
        list.add(element);
    }
}
