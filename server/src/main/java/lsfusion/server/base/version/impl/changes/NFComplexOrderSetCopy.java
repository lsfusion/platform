package lsfusion.server.base.version.impl.changes;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;

import java.util.List;
import java.util.function.IntFunction;

public class NFComplexOrderSetCopy<K> implements NFComplexOrderSetChange<K> {

    public final NFComplexOrderSet<K> col;
    public final NFCopy.Map<K> mapping;

    public NFComplexOrderSetCopy(NFComplexOrderSet<K> col, NFCopy.Map<K> mapping) {
        this.col = col;
        this.mapping = mapping;
    }

    // Generic group range finder for any "indexable" group list.
    // Assumes groups are ordered by value (as in original targetGroups logic).
    private static Pair<Integer, Integer> findGroupRange(int group, int size, IntFunction<Integer> groupAt) {
        int start = 0;
        while (start < size && groupAt.apply(start) < group)
            start++;
        int end = start;
        while (end < size && groupAt.apply(end) == group)
            end++;
        return new Pair<>(start, end);
    }

    // Generic "group block" range finder around a known index (contiguous block of equal group values).
    // This is used for sourceGroups where we rely on contiguity and already know sourceIndex belongs to sourceGroup.
    private static Pair<Integer, Integer> findGroupRangeAroundIndex(int index, int group, int size, IntFunction<Integer> groupAt) {
        int start = index;
        while (start > 0 && groupAt.apply(start - 1).equals(group))
            start--;

        int end = index + 1;
        while (end < size && groupAt.apply(end).equals(group))
            end++;

        return new Pair<>(start, end);
    }

    // Generic indexOf in a half-open range [range.first, range.second).
    private static <K> int indexOfInRange(IntFunction<K> at, Pair<Integer, Integer> range, K element) {
        for (int i = range.first; i < range.second; i++) {
            if (at.apply(i).equals(element))
                return i;
        }
        return -1;
    }

    private static <K> int findInsertIndexInGroup(int sourceIndex, int sourceGroup,
                                                  ImOrderSet<K> source, ImList<Integer> sourceGroups,
                                                  List<K> target, List<Integer> targetGroups) {

        // Range of this group in the current target (list/groupList).
        Pair<Integer, Integer> targetRange =
                findGroupRange(sourceGroup, targetGroups.size(), targetGroups::get);

        // Range of this group in the source (mapped/sourceGroups), computed locally around sourceIndex.
        Pair<Integer, Integer> sourceRange =
                findGroupRangeAroundIndex(sourceIndex, sourceGroup, sourceGroups.size(), sourceGroups::get);

        // 1) Prefer anchoring by the next element to the right within the same group:
        // if it already exists in target, insert BEFORE it.
        for (int j = sourceIndex + 1; j < sourceRange.second; j++) {
            int targetLocation = indexOfInRange(target::get, targetRange, source.get(j));
            if (targetLocation >= 0)
                return targetLocation;
        }

        // 2) Otherwise anchor by the previous element to the left within the same group:
        // insert AFTER it, but do not insert before "extra" target elements that are NOT in source.
        for (int j = sourceIndex - 1; j >= sourceRange.first; j--) {
            int targetLocation = indexOfInRange(target::get, targetRange, source.get(j));
            if (targetLocation >= 0) {
                int insertIndex = targetLocation + 1;

                // IMPORTANT CHANGE:
                // Skip elements inside the target group range that do NOT exist in the source group range.
                while (insertIndex < targetRange.second
                        && indexOfInRange(source::get, sourceRange, target.get(insertIndex)) < 0) {
                    insertIndex++;
                }
                return insertIndex;
            }
        }

        // 3) If there are no anchors at all, append to the end of the group range.
        return targetRange.second;
    }

    @Override
    public K getRemoveElement() {
        return null;
    }

    @Override
    public void proceedComplexOrderSet(List<K> list, List<Integer> groupList,
                                       NFComplexOrderSetChange<K> nextChange, Version version) {
        Pair<ImOrderSet<K>, ImList<Integer>> nf = col.getNFCopy(version);

        ImOrderSet<K> mapped = nf.first.mapOrderSetValues(mapping::apply);
        ImList<Integer> sourceGroups = nf.second;

        int size = mapped.size();
        for (int i = 0; i < size; i++) {
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
