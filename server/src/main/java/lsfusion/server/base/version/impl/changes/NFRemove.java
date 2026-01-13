package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.List;
import java.util.Set;

public class NFRemove<T> implements NFOrderSetChange<T>, NFComplexOrderSetChange<T> {
    private final T element;

    public NFRemove(T element) {
        this.element = element;
    }

    public void proceedSet(Set<T> mSet, Version version) {
        mSet.remove(element);
    }

    public void proceedOrderSet(List<T> list, Version version) {
        list.remove(element);
    }

    @Override
    public T getRemoveElement() {
        return element;
    }

    @Override
    public void proceedComplexOrderSet(List<T> list, List<Integer> groupList, NFComplexOrderSetChange<T> nextChange, Version version) {
        int index = list.indexOf(element);
        if(index >= 0) { // can be already removed (moved) in "parallel" module
            list.remove(index);
            groupList.remove(index);
        }
    }
}
