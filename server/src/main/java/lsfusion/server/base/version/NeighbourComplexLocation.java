package lsfusion.server.base.version;

import lsfusion.base.Pair;

import java.util.List;
import java.util.function.Function;

public class NeighbourComplexLocation<T> extends ComplexLocation<T> {

    public final T element;
    public final boolean isAfter;

    public NeighbourComplexLocation(T element, boolean isAfter) {
        this.element = element;
        this.isAfter = isAfter;
    }

    public Pair<Integer, Integer> getInsertGroup(List<T> list, List<Integer> groupList) {
        Integer elementGroup;
        int index = list.indexOf(element);
        int insertIndex;
        if (index < 0) { // the problem that neighbour can be moved from the container in a "parallel" module
            elementGroup = ComplexLocation.DEFAULTGROUP;
            insertIndex = list.size();
        } else {
            elementGroup = groupList.get(index);
            if (isAfter)
                index++;
            insertIndex = index;
        }
        return new Pair<>(insertIndex, elementGroup);
    }

    public boolean isReverseList() {
        return isAfter;
    }

    public <K> ComplexLocation<K> map(Function<T, K> mapNeighbour) {
        return new NeighbourComplexLocation<>(mapNeighbour.apply(element), isAfter);
    }
}
