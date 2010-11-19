package platform.base;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * User: DAle
 * Date: 18.11.2010
 * Time: 14:09:19
 */

public class Subsets<E> implements Iterable<Set<E>> {
    private Set<E> objects;
    
    public Subsets(Set<E> objects) {
        this.objects = objects;
    }


    public Iterator<Set<E>> iterator() {
        return new SubsetsIterator();
    }

    public class SubsetsIterator implements Iterator<Set<E>> {
        long subsetIndex;
        long subsetsCnt;

        SubsetsIterator() {
            subsetIndex = 0;
            subsetsCnt = 1 << objects.size();
        }

        public boolean hasNext() {
            return subsetIndex < subsetsCnt;
        }

        public Set<E> next() {
            Set<E> subset = new HashSet<E>();
            int index = 0;
            for (E object : objects) {
                if ((subsetIndex & (1 << index)) != 0) {
                    subset.add(object);
                }
                ++index;
            }
            ++subsetIndex;
            return subset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
