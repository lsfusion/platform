package platform.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListCombinations<V> implements Iterable<List<V>> {

    public Iterator<List<V>> iterator() {
        return new CombinationIterator();
    }

    List<List<V>> list = new ArrayList<List<V>>();
    public ListCombinations(List<List<V>> list) {
        this.list = list;
    }

    class CombinationIterator implements Iterator<List<V>> {

        int[] nums;
        int size;

        CombinationIterator() {
            for(List<V> innerList : list)
                if(innerList.size()==0) {
                    hasNext = false;
                    return;
                }
            size = list.size();
            nums = new int[size];
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }

        public List<V> next() {

            List<V> next = new ArrayList<V>();
            for(int i=0;i<size;i++)
                next.add(list.get(i).get(nums[i]));

            // переходим к следующей паре
            int i = 0;
            while(i<size && nums[i]== list.get(i).size()-1) {
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
