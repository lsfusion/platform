package lsfusion.base.col.lru;

import lsfusion.base.Pair;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.hash;

public class LRUWVWSMap<K, W, V> extends ALRUKWMap<MPair<K, W>, LRUWVWSMap.AEntry<K, W, V>, LRUWVWSMap.ASegment> {

    public LRUWVWSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    public interface Value<W, V> {
        W getLRUKey();
        V getLRUValue();
    }
    
    private final static Value notFound = new Value() {
        public Object getLRUKey() {
            return null;
        }
        public Object getLRUValue() {
            return null;
        }
    };
    public static <W, V> Value<W, V> notFound() {
        return notFound;
    }
        
    protected LRUWVWSMap.ASegment[] createSegments(int size) {
        return new LRUWVWSMap.ASegment[size];
    }

    protected LRUWVWSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public Value<W, V> get(K key) {
        recordOperation();
        int hash = hashKey(key);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(key, hash);
    }

    public void put(K key, W wValue, V value) {
        assert !key.getClass().isArray();

        recordOperation();
        int hash = hashKey(key);
        ASegment aSegment = segmentFor(hash);
        aSegment.put(key, hash, wValue, value);
    }

    private static <K> int hashKey(K key) {
        return hash(System.identityHashCode(key));
    }

    class ASegment extends ALRUKWMap<MPair<K, W>, AEntry<K, W, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<K, W, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<K, W, V> createTail() {
            return new AEntry<>(new MPair<K, W>(null, null), refQueue, null, -1, null, 0);
        }

        public final Value<W, V> get(K key, int hash) {
            final AEntry<K, W, V>[] t = table;
            for (AEntry<K, W, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                MPair<K, W> pair = e.get();
                if (pair != null && pair.first == key) {
                    recordAccess(e);
                    updateLRU();
                    return e;
                }
            }
            return null;
        }

        public V put(K key, int hash, W wValue, V value) {
            assert key != null && wValue!=null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<K, W, V> e = table[i]; e != null; e = e.next) {
                    MPair<K, W> pair = e.get();
                    if (pair != null && pair.first == key) {
                        assert pair.second == wValue; // раньше работало почему то без нижней строки
//                        assert false; // вообще по идее не должно быть так как pair.second и value не synchronized получаются (так как в get'е идет e а не копия)
                        pair.second = wValue;
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<K, W, V> e = new AEntry<>(key, wValue, refQueue, table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }

    }

    static class AEntry<K, W, V> extends ALRUKWMap.AEntry<MPair<K, W>, AEntry<K, W, V>> implements Value<W, V> {

        V value;

        AEntry(MPair<K, W> weak, ReferenceQueue<MPair<K, W>> refQueue, AEntry<K, W, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);

            this.value = value;
        }

        AEntry(K key, W weak, ReferenceQueue<MPair<K, W>> refQueue, AEntry<K, W, V> n, int hash, V value, int t) {
            this(new MPair<>(key, weak), refQueue, n, hash, value, t);
        }

        @Override
        public W getLRUKey() {
            MPair<K, W> value = get();
            if(value != null)
                return value.second;
            return null; 
        }

        public V getLRUValue() {
            return value;
        }
    }

}
