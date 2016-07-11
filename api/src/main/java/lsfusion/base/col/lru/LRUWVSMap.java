package lsfusion.base.col.lru;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.*;

public class LRUWVSMap<W, V> extends ALRUKWMap<W, LRUWVSMap.AEntry<W, V>, LRUWVSMap.ASegment> {

    public LRUWVSMap(Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWVSMap.ASegment[] createSegments(int size) {
        return new LRUWVSMap.ASegment[size];
    }

    protected LRUWVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(W wKey) {
        recordOperation();
        int hash = hashKey(wKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(wKey, hash);
    }

    public V put(W wKey, V value) {
        recordOperation();
        int hash = hashKey(wKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.put(wKey, hash, value);
    }
    
    private static <W> int hashKey(W wKey) {
        return hash(System.identityHashCode(wKey));
    }

    class ASegment extends ALRUKWMap<W, AEntry<W, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<W, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<W, V> createTail() {
            return new AEntry<>(weakTail(), refQueue, null, -1, null, 0);
        }

        public final V get(W wKey, int hash) {
            final AEntry<W, V>[] t = table;
            for (AEntry<W, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (e.get() == wKey) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(W wKey, int hash, V value) {
            assert wKey != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<W, V> e = table[i]; e != null; e = e.next) {
                    if (e.get() == wKey) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<W, V> e = new AEntry<>(wKey, refQueue, table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }

    }

    static class AEntry<W, V> extends ALRUKWMap.AEntry<W, AEntry<W, V>> {
        
        V value;

        AEntry(W weak, ReferenceQueue<W> refQueue, AEntry<W, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);
            
            this.value = value;
        }
    }
}
