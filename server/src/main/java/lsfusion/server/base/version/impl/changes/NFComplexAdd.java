package lsfusion.server.base.version.impl.changes;

import lsfusion.base.Pair;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;

import java.util.List;

// assert что есть
public class NFComplexAdd<T> implements NFComplexOrderSetChange<T> {
    private final T element;

    private final ComplexLocation<T> location;

    public NFComplexAdd(T element, ComplexLocation<T> location) {
        this.element = element;
        this.location = location;
    }

    @Override
    public T getRemoveElement() {
        return element;
    }

    @Override
    public void proceedComplexOrderSet(List<T> list, List<Integer> groupList, NFComplexOrderSetChange<T> nextChange, Version version) {
        T removeElement;
        if(nextChange != null) {
            removeElement = nextChange.getRemoveElement();
            if (removeElement != null && removeElement.equals(element))
                return;
        }

        int index = list.indexOf(element);
        if(index >= 0) {
            list.remove(index);
            groupList.remove(index);
        }

        Pair<Integer, Integer> insert;

        insert = location.getInsertGroup(list, groupList);

        list.add(insert.first, element);
        groupList.add(insert.first, insert.second);
    }
}
