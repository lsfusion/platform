package lsfusion.base;

import java.util.Iterator;

public class ArrayCombinations<V> implements Iterable<V[]> {

    public Iterator<V[]> iterator() {
        return new CombinationIterator();
    }

    private final V[][] list;
    private final ArrayInstancer<V> instancer;
    public final int max;
    public ArrayCombinations(V[][] list, ArrayInstancer<V> instancer) {
        this.list = list;
        this.instancer = instancer;
        int max = 1;
        for (V[] aList : list)
            max = max * aList.length;
        this.max = max;
    }

    class CombinationIterator implements Iterator<V[]> {

        int[] nums;
        int size;

        CombinationIterator() {
            for(V[] innerList : list)
                if(innerList.length==0) {
                    hasNext = false;
                    return;
                }
            size = list.length;
            nums = new int[size];
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }

        public V[] next() {

            V[] next = instancer.newArray(size);
            for(int i=0;i<size;i++)
                next[i] = list[i][nums[i]];

            // переходим к следующей паре
            int i = 0;
            while(i<size && nums[i]== list[i].length-1) {
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
