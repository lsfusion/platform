package lsfusion.server.base.version.impl.changes;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class NFRemoveAll<T> implements NFListChange<T>, NFOrderSetChange<T> {

    private final Predicate<T> remove;

    public NFRemoveAll(Predicate<T> remove) {
        this.remove = remove;
    }

    public void proceedList(MList<T> list, Version version) {
        list.removeAll(remove);
    }

    public void proceedCol(MCol<T> mCol, Version version) {
        mCol.removeAll(remove);
    }

    public void proceedOrderSet(List<T> list, Version version) {
        BaseUtils.removeList(list, remove);
    }

    public void proceedSet(Set<T> mSet, Version version) {
        BaseUtils.removeSet(mSet, remove);
    }
}
