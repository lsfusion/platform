package lsfusion.base.col.lru;

import lsfusion.base.Pair;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import static lsfusion.base.col.lru.LRUUtil.hash;

// WE means Weak but with normal equals, needed for some really rare cases, when we want to clean lru caches "near-immediately" on some event
public class LRUWWEVSMap<K, W, V> extends ALRUKWMap<K, LRUWWEVSMap.AEntry<K, W, V>, LRUWWEVSMap.ASegment> {

    public LRUWWEVSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    protected LRUWWEVSMap.ASegment[] createSegments(int size) {
        return new LRUWWEVSMap.ASegment[size];
    }

    protected LRUWWEVSMap.ASegment createSegment(int cap, float loadFactor) {
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
        return hash(31 * wKey.hashCode() + System.identityHashCode(sKey));
    }

    class ASegment extends ALRUKWMap<K, AEntry<K, W, V>, ASegment>.ASegment {

        protected ReferenceQueue<W> ref2Queue = new ReferenceQueue<>();

        @Override
        protected void updateLRU() {
            Reference<? extends W> poll = ref2Queue.poll();
            if(poll!=null) {
                changeLock.lock();
                try {
                    while(poll!=null) {
                        AEntry<K, W, V> pollEntry = ((AEntry<K,W,V>.Weak2Key) poll).getEntry();
                        if(pollEntry.isValid())
                            removeLRU(pollEntry);
                        poll = ref2Queue.poll();
                    }
                }
                finally{
                    changeLock.unlock();
                }
            }

            super.updateLRU();
        }

        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        @Override
        protected AEntry<K, W, V>[] createEntries(int size) {
            return new AEntry[size];
        }

        @Override
        protected AEntry<K, W, V> createTail() {
            return new AEntry<>(null, null, refQueue, ref2Queue, null, -1, null, 0);
        }

        public final V get(K sKey, W wKey, int hash) {
            final AEntry<K, W, V>[] t = (AEntry<K, W, V>[]) table;
            for (AEntry<K, W, V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                W ewKey;
                if (e.get() == sKey && (ewKey = e.key.get()) != null && optEquals(ewKey, wKey)) {
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
                for (AEntry<K, W, V> e = (AEntry<K, W, V> ) table[i]; e != null; e = e.next) {
                    W ewKey;
                    if (e.get() == sKey && (ewKey = e.key.get()) != null && optEquals(ewKey, wKey)) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                AEntry<K, W, V> e = new AEntry<K, W, V>(sKey, wKey, refQueue, ref2Queue, (AEntry<K, W, V>) table[i], hash, value, currentTime);

                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }
    }

    static class AEntry<K, W, V> extends ALRUKWMap.AEntry<K, AEntry<K, W, V>> {

        Weak2Key key;
        V value;

        class Weak2Key extends WeakReference<W> {

            public Weak2Key(W referent, ReferenceQueue<? super W> q) {
                super(referent, q);
            }

            public AEntry<K, W, V> getEntry() {
                return AEntry.this;
            }
        }

        AEntry(K key, W weak, ReferenceQueue<K> refQueue, ReferenceQueue<W> ref2Queue, AEntry<K, W, V> n, int hash, V value, int t) {
            super(key, refQueue, n, hash, t);

            this.key = new Weak2Key(weak, ref2Queue);
            this.value = value;
        }
    }
}
