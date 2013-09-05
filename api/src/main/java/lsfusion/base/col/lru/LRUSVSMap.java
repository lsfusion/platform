package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.hash;

public class LRUSVSMap<K, V> extends ALRUSMap<LRUSVSMap.AEntry<K, V>, LRUSVSMap.ASegment> {

    public LRUSVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUSVSMap.ASegment[] createSegments(int size) {
        return new LRUSVSMap.ASegment[size];
    }

    protected LRUSVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(K key) {
        recordOperation();
        int hash = hashKey(key);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(key, hash);
    }

    public V put(K key, V value) {
        assert !key.getClass().isArray();
        
        recordOperation();
        int hash = hashKey(key);
        ASegment aSegment = segmentFor(hash);
        return aSegment.put(key, hash, value);
    }

    private static <K> int hashKey(K key) {
        return hash(key.hashCode());
    }

    class ASegment extends ALRUSMap<AEntry<K, V>, ASegment>.ASegment {

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<K, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<K, V> createTail() {
            return new AEntry<K, V>(null, null, null, 0);
        }

        public final V get(K key, int hash) {
            final AEntry<K, V>[] t = table;
            for (AEntry<K, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (hashKey(e.key) ==hash && optEquals(e.key, key)) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(K key, int hash, V value) {
            assert key != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<K, V> e = table[i]; e != null; e = e.next) {
                    if (hashKey(e.key) ==hash && optEquals(e.key, key)) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<K, V> e = new AEntry<K, V>(key, table[i], value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }

    }

    static class AEntry<K, V> extends ALRUSMap.AEntry<AEntry<K, V>> {

        final K key;
        V value;

        AEntry(K key, AEntry<K, V> n, V value, int t) {
            super(n, t);

            this.key = key;
            this.value = value;
        }

        public int hashKey() {
            return LRUSVSMap.hashKey(key);
        }
    }
}
