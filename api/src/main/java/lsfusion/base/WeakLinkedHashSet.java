package lsfusion.base;

import java.util.*;

public class WeakLinkedHashSet<L> implements Iterable<L> {

    private int maxIndex = 0;
    private WeakHashMap<L, Integer> map = new WeakHashMap<L, Integer>();

    public void add(L item) {
        if(!map.containsKey(item))
            map.put(item, maxIndex++);
    }

    public Iterator<L> iterator() {
        SortedMap<Integer, L> list = new TreeMap<Integer, L>();
        for(Map.Entry<L,Integer> entry : map.entrySet())
            list.put(entry.getValue(), entry.getKey());
        return list.values().iterator();
    }
}
