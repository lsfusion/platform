package lsfusion.server.logics.mutables.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;

import java.util.List;
import java.util.Set;

public class NFRemoveAll<T> implements NFListChange<T>, NFOrderSetChange<T> {

    private NFRemoveAll() {
    }
    private static NFRemoveAll instance = new NFRemoveAll();
    public static <T> NFRemoveAll<T> getInstance() {
        return instance;
    }

    public void proceedList(MList<T> list) {
        list.removeAll();
    }

    public void proceedCol(MCol<T> mCol) {
        mCol.removeAll();
    }

    public void proceedOrderSet(List<T> list) {
        list.clear();
    }

    public void proceedSet(Set<T> mSet) {
        mSet.clear();
    }
}
