package platform.base;

import java.util.*;

public class Pairs<T,V> extends Permutations<Map<T,V>> {

    private List<T> from;
    private List<V> to;
    public Pairs(List<T> from, List<V> to) {
        super(from.size()==to.size()?from.size():-1);
        this.from = from;
        this.to = to;
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
