package platform.base;

import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetIndexValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListCombinations<V> implements Iterable<ImList<V>> {

    public Iterator<ImList<V>> iterator() {
        return new CombinationIterator();
    }

    ImList<ImList<V>> list;
    public ListCombinations(ImList<ImList<V>> list) {
        this.list = list;
    }

    class CombinationIterator implements Iterator<ImList<V>> {

        int[] nums;
        int size;

        CombinationIterator() {
            for(ImList<V> innerList : list)
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

        public ImList<V> next() {

            ImList<V> next = list.mapListValues(new GetIndexValue<V, ImList<V>>() {
                public V getMapValue(int i, ImList<V> value) {
                    return value.get(nums[i]);
                }});

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
