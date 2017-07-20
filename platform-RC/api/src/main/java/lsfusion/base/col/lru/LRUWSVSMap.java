package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.hash;

public class LRUWSVSMap<W, K, V> extends ALRUKWMap<W, LRUWSVSMap.AEntry<W, K, V>, LRUWSVSMap.ASegment> {

    public LRUWSVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWSVSMap.ASegment[] createSegments(int size) {
        return new LRUWSVSMap.ASegment[size];
    }

    protected LRUWSVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(W wKey, K sKey) {
        recordOperation();
        int hash = hashKey(wKey, sKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(wKey, sKey, hash);
    }

    public V put(W wKey, K sKey, V value) {
        assert !sKey.getClass().isArray();

        recordOperation();
        int hash = hashKey(wKey, sKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.put(wKey, sKey, hash, value);
    }

    private static <W, K> int hashKey(W wKey, K sKey) {
        return hash(31 * System.identityHashCode(wKey) + sKey.hashCode());
    }

    class ASegment extends ALRUKWMap<W, AEntry<W, K, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<W, K, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<W, K, V> createTail() {
            return new AEntry<>(weakTail(), null, refQueue, null, -1, null, 0);
        }

        public final V get(W wKey, K sKey, int hash) {
            final AEntry<W, K, V>[] t = table;
            for (AEntry<W, K, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (e.get() == wKey && e.hash==hash && optEquals(e.key, sKey)) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(W wKey, K sKey, int hash, V value) {
            assert wKey != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<W, K, V> e = table[i]; e != null; e = e.next) {
                    if (e.get() == wKey && e.hash==hash && optEquals(e.key, sKey)) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<W, K, V> e = new AEntry<>(wKey, sKey, refQueue, table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }

    }

    static class AEntry<W, K, V> extends ALRUKWMap.AEntry<W, AEntry<W, K, V>> {

        final K key;
        V value;

        AEntry(W weak, K key, ReferenceQueue<W> refQueue, AEntry<W, K, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);

            this.key = key;
            this.value = value;
        }
    }
}
