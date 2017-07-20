package lsfusion.base.col.lru;

import lsfusion.base.Pair;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.hash;

public class LRUWWVSMap<K, W, V> extends ALRUKWMap<Pair<K, W>, LRUWWVSMap.AEntry<K, W, V>, LRUWWVSMap.ASegment> {

    public LRUWWVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWWVSMap.ASegment[] createSegments(int size) {
        return new LRUWWVSMap.ASegment[size];
    }

    protected LRUWWVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(K sKey, W wKey) {
        recordOperation();
        int hash = hashKey(sKey, wKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(sKey, wKey, hash);
    }

    public void put(K sKey, W wKey, V value) {
        assert !sKey.getClass().isArray();

        recordOperation();
        int hash = hashKey(sKey, wKey);
        ASegment aSegment = segmentFor(hash);
        aSegment.put(sKey, wKey, hash, value);
    }

    private static <W, K> int hashKey(K sKey, W wKey) {
        return hash(31 * System.identityHashCode(wKey) + System.identityHashCode(sKey));
    }

    class ASegment extends ALRUKWMap<Pair<K, W>, AEntry<K, W, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<K, W, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<K, W, V> createTail() {
            return new AEntry<>(new Pair<K, W>(null, null), refQueue, null, -1, null, 0);
        }

        public final V get(K sKey, W wKey, int hash) {
            final AEntry<K, W, V>[] t = table;
            for (AEntry<K, W, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                Pair<K, W> pair = e.get();
                if (pair != null && pair.first == sKey && pair.second == wKey) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(K sKey, W wKey, int hash, V value) {
            assert sKey != null && wKey != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<K, W, V> e = table[i]; e != null; e = e.next) {
                    Pair<K, W> pair = e.get();
                    if (pair != null && pair.first == sKey && pair.second == wKey) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<K, W, V> e = new AEntry<>(sKey, wKey, refQueue, table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }
    }

    static class AEntry<K, W, V> extends ALRUKWMap.AEntry<Pair<K, W>, AEntry<K, W, V>> {

        V value;

        AEntry(Pair<K, W> weak, ReferenceQueue<Pair<K, W>> refQueue, AEntry<K, W, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);

            this.value = value;
        }

        AEntry(K key, W weak, ReferenceQueue<Pair<K, W>> refQueue, AEntry<K, W, V> n, int hash, V value, int t) {
            this(new Pair<>(key, weak), refQueue, n, hash, value, t);
        }
    }
}
