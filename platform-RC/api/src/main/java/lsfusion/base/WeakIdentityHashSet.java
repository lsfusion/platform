package lsfusion.base;

import java.util.HashSet;
import java.util.Iterator;

public class WeakIdentityHashSet<E> implements Iterable<E> {

    final static Object inSet = 1;
    WeakIdentityHashMap<E,Object> map = new WeakIdentityHashMap<>();

    public WeakIdentityHashSet(E element) {
        map.put(element,inSet);
    }

    public WeakIdentityHashSet() {
    }

    public void add(E element) {
        map.put(element, inSet);        
    }

    public void remove(E element) {
        map.remove(element);
    }

    public Iterator<E> iterator() {
        return map.keysIterator();
    }

    public void addAll(WeakIdentityHashSet<E> set) {
        map.putAll(set.map);
    }

    public boolean contains(E element) {
        return map.get(element)!=null;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public HashSet<E> copy() {
        HashSet<E> result = new HashSet<>();
        for(E element : this)
            result.add(element);
        return result;
    }
}
