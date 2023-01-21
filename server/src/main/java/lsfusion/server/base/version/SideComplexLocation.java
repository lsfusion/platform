package lsfusion.server.base.version;

import lsfusion.base.Pair;

import java.util.List;
import java.util.function.Function;

public class SideComplexLocation<T> extends ComplexLocation<T> {

    public final boolean isFirst;
    public final int group;

    public SideComplexLocation(boolean isFirst, int group) {
        this.isFirst = isFirst;
        this.group = group;
    }

    public Pair<Integer, Integer> getInsertGroup(List<T> list, List<Integer> groupList) {
        int insertIndex;
        if(isFirst) { // looking for the first
            int i = 0;
            while(i < list.size() && group > groupList.get(i))
                i++;
            insertIndex = i;
        } else { // looking for the last
            int i = list.size();
            while(i > 0 && group < groupList.get(i - 1))
                i--;
            insertIndex = i;
        }
        return new Pair<>(insertIndex, group);
    }

    public boolean isReverseList() {
        return isFirst;
    }

    @Override
    public <K> ComplexLocation<K> map(Function<T, K> mapNeighbour) {
        return (ComplexLocation<K>) this;
    }
}
