package platform.base;

import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
