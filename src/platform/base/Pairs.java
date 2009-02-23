package platform.base;

import java.util.*;

public class Pairs<T,V> extends Permutations<Map<T,V>> {

    List<T> from;
    List<V> to;
    public Pairs(Collection<? extends T> iFrom, Collection<? extends V> iTo) {
        from = new ArrayList<T>(iFrom);
        to = new ArrayList<V>(iTo);
        if((size=from.size())!=to.size())
            size = -1;
    }

    Map<T, V> getPermute(PermuteIterator permute) {
        Map<T,V> next = new HashMap<T,V>();
        for(int i=0;i<size;i++)
            next.put(from.get(i),to.get(permute.nums[i]));
        return next;
    }

    boolean isEmpty() {
        return from.size()!=to.size();
    }
}
