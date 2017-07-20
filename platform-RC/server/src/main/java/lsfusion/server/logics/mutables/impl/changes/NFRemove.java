package lsfusion.server.logics.mutables.impl.changes;

import java.util.List;
import java.util.Set;

public class NFRemove<T> implements NFOrderSetChange<T> {
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
}
