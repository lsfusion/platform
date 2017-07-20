package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;

import java.lang.ref.ReferenceQueue;
import java.util.Arrays;

import static lsfusion.base.col.lru.LRUUtil.hash;

// 3-й параметр массив (у него другие equals и hashCode)
public class LRUWSASVSMap<W, K, E, V> extends ALRUKWMap<W, LRUWSASVSMap.AEntry<W, K, E, V>, LRUWSASVSMap.ASegment> {

    public LRUWSASVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWSASVSMap.ASegment[] createSegments(int size) {
        return new LRUWSASVSMap.ASegment[size];
    }

    protected LRUWSASVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(W wKey, K sKey, E[] eKey) {
        recordOperation();
        int hash = hashKey(wKey, sKey, eKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(wKey, sKey, eKey, hash);
    }

    public V put(W wKey, K sKey, E[] eKey, V value) {
        assert !sKey.getClass().isArray();

        recordOperation();
        int hash = hashKey(wKey, sKey, eKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.put(wKey, sKey, eKey, hash, value);
    }

    private static <W, K, E> int hashKey(W wKey, K sKey, E[] eKey) {
        return hash(31 * (31 * System.identityHashCode(wKey) + sKey.hashCode()) + Arrays.hashCode(eKey));
    }

    class ASegment extends ALRUKWMap<W, AEntry<W, K, E, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<W, K, E, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<W, K, E, V> createTail() {
            return new AEntry<>(weakTail(), null, null, refQueue, null, -1, null, 0);
        }

        public final V get(W wKey, K sKey, E[] eKey, int hash) {
            final AEntry<W, K, E, V>[] t = table;
            for (AEntry<W, K, E, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (e.get() == wKey && e.hash == hash && optEquals(e.key, sKey) && Arrays.equals(e.eKey, eKey)) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(W wKey, K sKey, E[] eKey, int hash, V value) {
            assert wKey != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<W, K, E, V> e = table[i]; e != null; e = e.next) {
                    if (e.get() == wKey && e.hash == hash && optEquals(e.key, sKey) && Arrays.equals(e.eKey, eKey)) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<W, K, E, V> e = new AEntry<>(wKey, sKey, eKey, refQueue, table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }

    }

    static class AEntry<W, K, E, V> extends ALRUKWMap.AEntry<W, AEntry<W, K, E, V>> {

        final K key;
        final E[] eKey;
        V value;

        AEntry(W weak, K key, E[] eKey, ReferenceQueue<W> refQueue, AEntry<W, K, E, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);

            this.key = key;
            this.eKey = eKey;
            this.value = value;
        }
    }
}