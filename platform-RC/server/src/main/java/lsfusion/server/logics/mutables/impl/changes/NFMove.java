package lsfusion.server.logics.mutables.impl.changes;

import lsfusion.server.logics.mutables.FindIndex;

import java.util.List;
import java.util.Set;

// assert что есть
public class NFMove<T> implements NFOrderSetChange<T> {
    private final T element;    
    private final FindIndex<T> finder;

    public NFMove(T element, FindIndex<T> finder) {
        this.element = element;
        this.finder = finder;
    }

    public void proceedSet(Set<T> mSet) {
    }

    public void proceedOrderSet(List<T> list) {
        list.remove(element);
        list.add(finder.getIndex(list), element);
    }
}
