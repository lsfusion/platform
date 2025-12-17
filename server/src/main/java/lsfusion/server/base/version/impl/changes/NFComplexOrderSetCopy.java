package lsfusion.server.base.version.impl.changes;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;

import java.util.List;

public class NFComplexOrderSetCopy<K> implements NFComplexOrderSetChange<K> {

    public final NFComplexOrderSet<K> col;
    public final NFCopy.Map<K> mapping;

    public NFComplexOrderSetCopy(NFComplexOrderSet<K> col, NFCopy.Map<K> mapping) {
        this.col = col;
        this.mapping = mapping;
    }

    @SuppressWarnings("unchecked")
    private K[] mapAll(ImOrderSet<K> source) {
        int size = source.size();
        K[] mapped = (K[]) new Object[size];
        for(int i = 0; i < size; i++)
            mapped[i] = mapping.apply(source.get(i));
        return mapped;
    }

    private static Pair<Integer, Integer> findGroupRange(int group, List<Integer> groupList) {
        int start = 0;
        while(start < groupList.size() && groupList.get(start) < group)
            start++;
        int end = start;
        while(end < groupList.size() && groupList.get(end) == group)
            end++;
        return new Pair<>(start, end);
    }

    private static <K> int indexOf(List<K> target, K element, Pair<Integer, Integer> range) {
        for(int i = range.first; i < range.second; i++) {
            if(target.get(i).equals(element))
                return i;
        }
        return -1;
    }
    private static <K> int findInsertIndexInGroup(int sourceIndex, int sourceGroup, ImOrderSet<K> source, ImList<Integer> sourceGroups, List<K> target, List<Integer> targetGroups) {
        Pair<Integer, Integer> range = findGroupRange(sourceGroup, targetGroups);

        for(int j = sourceIndex + 1, size = source.size(); j < size; j++) {
            if(!sourceGroups.get(j).equals(sourceGroup))
                break;

            int targetLocation = indexOf(target, source.get(j), range);
            if(targetLocation >= 0)
                return targetLocation;
        }
        for(int j = sourceIndex - 1; j >= 0; j--) {
            if(!sourceGroups.get(j).equals(sourceGroup))
                break;

            int targetLocation = indexOf(target, source.get(j), range);
            if(targetLocation >= 0)
                return targetLocation + 1;

        }
        return range.second;
    }

    @Override
    public void proceedComplexOrderSet(List<K> list, List<Integer> groupList, Version version) {
        Pair<ImOrderSet<K>, ImList<Integer>> nf = col.getNFCopy(version);

        ImOrderSet<K> mapped = nf.first.mapOrderSetValues(mapping::apply);
        ImList<Integer> sourceGroups = nf.second;
        int size = mapped.size();
        for(int i = 0; i < size; i++) {
            K element = mapped.get(i);
            if (!list.contains(element)) {
                int group = sourceGroups.get(i);

                int insertIndex = findInsertIndexInGroup(i, group, mapped, sourceGroups, list, groupList);

                list.add(insertIndex, element);
                groupList.add(insertIndex, group);
            }
        }
    }
}
