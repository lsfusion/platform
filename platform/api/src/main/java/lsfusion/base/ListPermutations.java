package lsfusion.base;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;

public class ListPermutations<T> extends Permutations<ImOrderSet<T>> {

    ImOrderSet<T> to;

    public ListPermutations(ImOrderSet<T> toCollection) {
        super(toCollection.size());
        to = toCollection;
    }

    ImOrderSet<T> getPermute(final PermuteIterator permute) {
        return SetFact.toOrderExclSet(size, new GetIndex<T>() {
            public T getMapValue(int i) {
                return to.get(permute.nums[i]);
            }});
    }
}
