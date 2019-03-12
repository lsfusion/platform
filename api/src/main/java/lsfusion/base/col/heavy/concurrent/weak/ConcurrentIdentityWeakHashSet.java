package lsfusion.base.col.heavy.concurrent.weak;

import lsfusion.base.BaseUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ConcurrentIdentityWeakHashSet<K> implements Set<K> {
    
    private final ConcurrentIdentityWeakHashMap<K, Integer> map = new ConcurrentIdentityWeakHashMap<>();
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.contains(o);
    }

    @Override
    public Iterator<K> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);    
    }

    @Override
    public boolean add(K k) {
        return map.put(k, 0) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
        map.putAll(BaseUtils.toMap(c, 0));
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();    }

    @Override
    public void clear() {
        map.clear();
    }
}
