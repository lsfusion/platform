package platform.base.col.lru;

public class ThreadBucket {
    private ArLRUIndexedMap[][] caches;
    private int[] sizes;

    private final static int bucketSize = 100;
    public ThreadBucket() {
        caches = new ArLRUIndexedMap[LRUCache.stratCount][];
        for(int i=0;i<caches.length;i++)
            caches[i] = new ArLRUIndexedMap[bucketSize];
        sizes = new int[caches.length];
    }

    public void add(ArLRUIndexedMap map, byte strategy) {
        if(caches[strategy].length == sizes[strategy]) {
            flush(strategy);
            sizes[strategy] = 0;
            caches[strategy] = new ArLRUIndexedMap[bucketSize];
        }
        caches[strategy][sizes[strategy]++] = map;
    }

    private void flush(byte strategy) {
        LRUCache.addPending(caches[strategy], strategy);
    }
    
    protected void finalize() throws Throwable {
        super.finalize();
        for(byte i=0;i<caches.length;i++)
            flush(i);
    }
}
