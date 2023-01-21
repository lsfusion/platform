package lsfusion.server.base.version.impl.changes;

import lsfusion.base.Pair;
import lsfusion.server.base.version.ComplexLocation;

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
    public void proceedComplexOrderSet(List<T> list, List<Integer> groupList) {
        Pair<Integer, Integer> insert;

        insert = location.getInsertGroup(list, groupList);

        list.add(insert.first, element);
        groupList.add(insert.first, insert.second);
    }
}
