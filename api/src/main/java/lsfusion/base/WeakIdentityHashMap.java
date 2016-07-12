package lsfusion.base;

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
    
    public boolean containsKey(K key) {
        expunge();
        return map.containsKey(new IdentityWeakReference<K>(key));
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

    public void clear() {
        map.clear();
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

    public Iterator<K> keysIterator() {
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
    
    public boolean disjointKeys(WeakIdentityHashMap<K, V> map) {
        for(K key : keysIt())
            if (map.get(key) != null)
                return false;
        return true;
    }
    
    public Iterable<K> keysIt() {
        return new Iterable<K>() {
            public Iterator<K> iterator() {
                return keysIterator();
            }
        };
    };

    public Iterator<Pair<K, V>> entryIterator() {
        expunge();
        final Iterator<Map.Entry<WeakReference<K>, V>> it = map.entrySet().iterator();
        return new Iterator<Pair<K, V>>() {
            Pair<K, V> next = null;
            public boolean hasNext() {
                while(next==null) {
                    if(it.hasNext()) {
                        Map.Entry<WeakReference<K>, V> itNext = it.next();
                        K key = itNext.getKey().get();
                        if(key == null)
                            next = null;
                        else
                            next = new Pair<K, V>(key, itNext.getValue());
                    }
                    else
                        return false;
                }
                return true;
            }

            public Pair<K, V> next() {
                Pair<K, V> result = next;
                next = null;
                return result;
            }

            public void remove() {
                throw new RuntimeException("not supported");
            }
        };
    }
    
    public Iterable<Pair<K, V>> entryIt() {
        return new Iterable<Pair<K, V>>() {
            public Iterator<Pair<K, V>> iterator() {
                return entryIterator();
            }
        }; 
    }
}
