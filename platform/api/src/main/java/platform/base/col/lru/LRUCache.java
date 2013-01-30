package platform.base.col.lru;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.implementations.ArIndexedMap;
import platform.base.col.implementations.HMap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LRUCache {

    public static void addPending(ArLRUIndexedMap[] caches, byte strategy) {
        strategies[strategy].addPending(caches);
    }

    public static byte stratCount = 2;
    public static final byte EXP_QUICK = 0;
    public static final byte EXP_RARE = 1;
    private static Strategy[] strategies = new Strategy[stratCount];
    public static void init(int[] cleanPeriods, int[] expireSeconds, int[] proceedBuckets) {
        for(byte i=0;i<stratCount;i++) {
            if(cleanPeriods[i] > 0)
                strategies[i] = new Strategy(cleanPeriods[i], expireSeconds[i], proceedBuckets[i]);
        }
    }

    public static <K, V> MCacheMap<K, V> mBig() {
        return new HMap<K, V>(MapFact.<K, V>exclusive());
    }

/*    private static class SyncCache<K, V> implements MCacheMap<K, V> {
        private final MCacheMap<K, V> syncMap;

        private SyncCache(MCacheMap<K, V> syncMap) {
            this.syncMap = syncMap;
        }

        public synchronized V get(K key) {
            return syncMap.get(key);
        }

        public synchronized boolean containsKey(K key) {
            return syncMap.containsKey(key);
        }

        public synchronized void exclAdd(K key, V value) {
            syncMap.exclAdd();
        }
    }*/

    // предполагается что все использования должны synchronized'ся
    public static <K, V> MCacheMap<K, V> mSmall(byte strategy) {
        if(strategies[strategy]==null)
            return new ArIndexedMap<K, V>(MapFact.<K, V>exclusive());
        return new ArLRUIndexedMap<K, V>(strategy);
    }

    private static class Strategy extends TimerTask {
        public void addPending(ArLRUIndexedMap[] caches) {
            synchronized (pendingBuckets) {
                pendingBuckets.add(caches);
            }
        }

        private final List<ArLRUIndexedMap[]> pendingBuckets = new ArrayList<ArLRUIndexedMap[]>();// synchronized
        private List<WeakReference<ArLRUIndexedMap>> fixedCaches = new ArrayList<WeakReference<ArLRUIndexedMap>>();

        private int prevNotProceeded;
        private int nulls = 0;

        private int proceedBucket = 20000;
        private float factorNullsPack = 0.5f;
        private int expireSecond = 5;

        public void run() {
            synchronized (pendingBuckets) {
                for(ArLRUIndexedMap[] bucket : pendingBuckets) {
                    for (ArLRUIndexedMap map : bucket) {
                        if (map == null)
                            break;
                        fixedCaches.add(new WeakReference<ArLRUIndexedMap>(map));
                    }
                }
                pendingBuckets.clear();
            }

            long currentTime = System.nanoTime();
            int expired = (int) ((currentTime - (expireSecond << 30)) >> 32);

            int index = prevNotProceeded;
            for(int i=0;i<BaseUtils.min(proceedBucket,fixedCaches.size());i++) {
                WeakReference<ArLRUIndexedMap> weakFixedCache = fixedCaches.get(index);
                if(weakFixedCache!=null) {
                    ArLRUIndexedMap fixedCache = weakFixedCache.get();
                    if(fixedCache == null) { // deleted
                        fixedCaches.set(index, null);
                        nulls++;
                    } else
                        fixedCache.removeExpired(expired);
                }

                index++;
                if(index == fixedCaches.size())
                    index = 0;
            }
            prevNotProceeded = index;

            if(nulls > fixedCaches.size() * factorNullsPack) {
                List<WeakReference<ArLRUIndexedMap>> packFixedCaches = new ArrayList<WeakReference<ArLRUIndexedMap>>(fixedCaches.size()-nulls);
                for(WeakReference<ArLRUIndexedMap> weakFixedCache : fixedCaches)
                    if(weakFixedCache!=null)
                        packFixedCaches.add(weakFixedCache);
                fixedCaches = packFixedCaches;
                nulls = 0;

                if(prevNotProceeded >= fixedCaches.size())
                    prevNotProceeded = 0;
            }
        }

        private Timer cleanTimer;
        private Strategy(int cleanPeriod, int expireSecond, int proceedBucket) {
            this.expireSecond = expireSecond;
            this.proceedBucket = proceedBucket;

            cleanTimer = new Timer(true);
            cleanTimer.schedule(this, 0, cleanPeriod * 1000);
        }
    }
}
