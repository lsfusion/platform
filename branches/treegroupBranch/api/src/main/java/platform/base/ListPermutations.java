package platform.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListPermutations<T> extends Permutations<List<T>> {

    List<T> to;

    public ListPermutations(Collection<T> toCollection) {
        super(toCollection.size());
        to = new ArrayList<T>(toCollection);
    }

    List<T> getPermute(PermuteIterator permute) {
        List<T> result = new ArrayList<T>();
        for(int i=0;i<size;i++)
            result.add(to.get(permute.nums[i]));
        return result;
    }
}
