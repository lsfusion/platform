package lsfusion.base;

import java.util.*;

public class Combinations<T,V> implements Iterable<Map<T,V>> {

    public Iterator<Map<T, V>> iterator() {
        return new CombinationIterator();
    }

    List<T> from = new ArrayList<>();
    List<List<V>> to = new ArrayList<>();
    public Combinations(Map<T, List<V>> map) {
        for(Map.Entry<T,List<V>> entry : map.entrySet()) {
            from.add(entry.getKey());
            to.add(entry.getValue());
        }
    }

    class CombinationIterator implements Iterator<Map<T,V>> {

        int[] nums;
        int size;

        CombinationIterator() {
            for(List<V> list : to)
                if(list.size()==0) {
                    hasNext = false;
                    return;
                }
            size = from.size();
            nums = new int[size];
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }

        public Map<T, V> next() {

            Map<T,V> next = new HashMap<>();
            for(int i=0;i<size;i++)
                next.put(from.get(i),to.get(i).get(nums[i]));

            // переходим к следующей паре
            int i = 0;
            while(i<size && nums[i]==to.get(i).size()-1) {
                nums[i] = 0;
                i++;
            }
            if(i==size)
                hasNext = false;
            else
                nums[i]++;

            return next;
        }

        public void remove() { // не поддерживает
        }
    }
}
