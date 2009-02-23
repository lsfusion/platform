package platform.base;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

public class ListPermutations<T> extends Permutations<List<T>> {

    List<T> to;

    public ListPermutations(Collection<T> toCollection) {
        to = new ArrayList<T>(toCollection);
        size = to.size();
    }

    List<T> getPermute(PermuteIterator permute) {
        List<T> result = new ArrayList<T>();
        for(int i=0;i<size;i++)
            result.add(to.get(permute.nums[i]));
        return result;
    }
}
