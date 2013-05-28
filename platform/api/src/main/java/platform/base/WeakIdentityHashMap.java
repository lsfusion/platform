package platform.base;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WeakIdentityHashMap<K, V> {
    public WeakIdentityHashMap() {}

    private Map<WeakReference<K>, V> map = new HashMap<WeakReference<K>, V>();
    private ReferenceQueue<K> refQueue = new ReferenceQueue<K>();

    public V get(K key) {
        expunge();
        return map.get(new IdentityWeakReference<K>(key));
    }

    public V put(K key, V value) {
        expunge();
        return map.put(new IdentityWeakReference<K>(key, refQueue), value);
    }

    public void putAll(WeakIdentityHashMap<K, V> weak) {
        map.putAll(weak.map);
    }

    public V remove(K key) {
        expunge();
        return map.remove(new IdentityWeakReference<K>(key));
    }

    public int size() {
        expunge();
        return map.size();
    }

    public boolean isEmpty() {
        expunge();
        return map.isEmpty();
    }

    private void expunge() {
        Reference<? extends K> ref;
        while ((ref = refQueue.poll()) != null)
            map.remove((IdentityWeakReference)ref);
    }

    private static class IdentityWeakReference<T> extends WeakReference<T> {

        IdentityWeakReference(T o) {
            this(o, null);
        }

        private final int hashCode;
        IdentityWeakReference(T o, ReferenceQueue<T> q) {
            super(o, q);
            this.hashCode = (o == null) ? 0 : System.identityHashCode(o);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdentityWeakReference)) return false;
            Object got = get();
            return (got != null && got == ((IdentityWeakReference) o).get());
        }

        public int hashCode() {
            return hashCode;
        }
    }

    Iterator<K> keysIterator() {
        expunge();
        final Iterator<WeakReference<K>> it = map.keySet().iterator();
        return new Iterator<K>() {
            K next = null;
            public boolean hasNext() {
                while(next==null) {
                    if(it.hasNext())
                        next = it.next().get();
                    else
                        return false;
                }
                return true;
            }

            public K next() {
                K result = next;
                next = null;
                return result;
            }

            public void remove() {
                throw new RuntimeException("not supported");
            }
        };
    }
}
