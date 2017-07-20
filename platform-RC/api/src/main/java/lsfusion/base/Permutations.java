package lsfusion.base;

import java.util.Iterator;

public abstract class Permutations<Permute> implements Iterable<Permute> {

    int size;

    protected Permutations(int size) {
        this.size = size;
    }

    public Iterator<Permute> iterator() {
        return new PermuteIterator();
    }

    abstract Permute getPermute(PermuteIterator permute);

    class PermuteIterator implements Iterator<Permute> {

        int[] nums;

        PermuteIterator() {
            if(size<0)
                hasNext = false;
            else {
                nums = new int[size];
                for(int i=0;i<size;i++) // начальная перестановка
                    nums[i] = i;
            }
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }

        public Permute next() {

            Permute next = getPermute(this);

            int i=size-1;
            while(i>=1 && nums[i-1]>nums[i]) i--; // находим первый нарушающий порядок
            if(i<=0)
                hasNext = false;
            else {
                // находим минимальный элемент больше это
                int min = i;
                for(int j=i+1;j<size;j++)
                    if(nums[j]>nums[i-1] && nums[j]<nums[min])
                        min = j;
                int t = nums[i-1]; nums[i-1] = nums[min]; nums[min] = t;
                for(int j=0;j<(size-i)/2;j++) { // переворачиваем
                    t = nums[i+j]; nums[i+j] = nums[size-1-j]; nums[size-1-j] = t; }
            }

            return next;
        }

        public void remove() { // не поддерживает
        }
    }
}
