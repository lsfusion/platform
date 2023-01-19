package lsfusion.server.base.version.impl.changes;

import java.util.List;
import java.util.Set;

public class NFRemove<T> implements NFOrderSetChange<T>, NFComplexOrderSetChange<T> {
    private final T element;

    public NFRemove(T element) {
        this.element = element;
    }

    public void proceedSet(Set<T> mSet) {
        mSet.remove(element);
    }

    public void proceedOrderSet(List<T> list) {
        list.remove(element);
    }

    @Override
    public void proceedComplexOrderSet(List<T> list, List<Integer> groupList) {
        int index = list.indexOf(element);
        if(index >= 0) { // can be already removed (moved) in "parallel" module
            list.remove(index);
            groupList.remove(index);
        }
    }
}
