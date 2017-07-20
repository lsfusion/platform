package lsfusion.base.col.lru;

import lsfusion.base.DProcessor;
import lsfusion.base.Processor;

import java.lang.ref.ReferenceQueue;

import static lsfusion.base.col.lru.LRUUtil.hash;

public class LRUWSSVSMap<W, K, E, V> extends ALRUKWMap<W, LRUWSSVSMap.AEntry<W, K, E, V>, LRUWSSVSMap.ASegment> {

    public LRUWSSVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWSSVSMap.ASegment[] createSegments(int size) {
        return new LRUWSSVSMap.ASegment[size];
    }

    protected LRUWSSVSMap.ASegment createSegment(int cap, float loadFactor) {
        return new ASegment(cap, loadFactor);
    }

    // get и put - copy paste с изменением кол-ва параметров
    public V get(W wKey, K sKey, E eKey) {
        recordOperation();
        int hash = hashKey(wKey, sKey, eKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.get(wKey, sKey, eKey, hash);
    }

    public V put(W wKey, K sKey, E eKey, V value) {
        assert !sKey.getClass().isArray();

        recordOperation();
        int hash = hashKey(wKey, sKey, eKey);
        ASegment aSegment = segmentFor(hash);
        return aSegment.put(wKey, sKey, eKey, hash, value);
    }

    private static <W, K, E> int hashKey(W wKey, K sKey, E eKey) {
        return hash(31 * (31 * System.identityHashCode(wKey) + sKey.hashCode()) + eKey.hashCode());
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

        public final V get(W wKey, K sKey, E eKey, int hash) {
            final AEntry<W, K, E, V>[] t = table;
            for (AEntry<W, K, E, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (e.get() == wKey && e.hash == hash && optEquals(e.key, sKey) && optEquals(e.eKey, eKey)) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(W wKey, K sKey, E eKey, int hash, V value) {
            assert wKey != null && value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (AEntry<W, K, E, V> e = table[i]; e != null; e = e.next) {
                    if (e.get() == wKey && e.hash == hash && optEquals(e.key, sKey) && optEquals(e.eKey, eKey)) {
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

    public void proceedSafeLockLRUEKeyValues(final DProcessor<E, V> processor) {
        proceedSafeLockLRUEEntries(new Processor<AEntry<W, K, E, V>>() {
            public void proceed(AEntry<W, K, E, V> element) {
                processor.proceed(element.eKey, element.value);
            }
        });
    }

    static class AEntry<W, K, E, V> extends ALRUKWMap.AEntry<W, AEntry<W, K, E, V>> {

        final K key;
        final E eKey;
        V value;

        AEntry(W weak, K key, E eKey, ReferenceQueue<W> refQueue, AEntry<W, K, E, V> n, int hash, V value, int t) {
            super(weak, refQueue, n, hash, t);

            this.key = key;
            this.eKey = eKey;
            this.value = value;
        }
    }
}
