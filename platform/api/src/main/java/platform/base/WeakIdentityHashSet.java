package platform.base;

import java.util.Iterator;

public class WeakIdentityHashSet<E> implements Iterable<E> {

    final static Object inSet = 1;
    WeakIdentityHashMap<E,Object> map = new WeakIdentityHashMap<E,Object>();

    public WeakIdentityHashSet(E element) {
        map.put(element,inSet);
    }

    public WeakIdentityHashSet() {
    }

    public void add(E element) {
        map.put(element, inSet);        
    }

    public Iterator<E> iterator() {
        return map.keysIterator();
    }

    public void addAll(WeakIdentityHashSet<E> set) {
        map.putAll(set.map);
    }
}
