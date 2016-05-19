package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.interop.DaemonThreadFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LRUUtil {
    private static final int UPDATE_CURTIME_FREQUENCY = 1 << 7;
    static final int UPDATE_CURTIME_FREQUENCY_MASK = UPDATE_CURTIME_FREQUENCY - 1;
    private final static Object lockCleanLRU = new Object();
    
    public static double multiplier = 1.0;
    public static double MAX_MULTIPLIER = 15.0; // чтобы не рос до бесконечности, из - за того что некоторые кэши имеют максимальные пределы

    public static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }

    public static int nearestPowerOf2(int num) {
        int res = 1;
        while (res < num) {
            res <<= 1;
        }
        return res;
    }

    private static MemoryPoolMXBean getTenuredPool() {
        MemoryPoolMXBean tenuredGenPool = null;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
                tenuredGenPool = pool;
            }
        }
        return tenuredGenPool;
    }

    public static double memGCIn100Millis = 0.1; // сколько процентов памяти может собрать сборщик мусора за 100 мс

    private static ScheduledExecutorService scheduler;
    private static long lastCollected;
    private static boolean runningCleanLRU = false;

    private static final String cmsFraction = "-XX:CMSInitiatingOccupancyFraction=";
    private static final String useGCFraction = "-XX:+UseG1GC";
    public static void initLRUTuner(final LRULogger logger) {
        final MemoryPoolMXBean tenuredGenPool = getTenuredPool();
        double averageRate = 0.7;
        double safeMem = 0.1;
        boolean useG1GC = false;
        for(String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith(cmsFraction)) {
                double cmsFractionValue = 0.0;
                try {
                    cmsFractionValue = Double.valueOf(arg.substring(cmsFraction.length())) / 100.0;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if(cmsFractionValue >= safeMem)
                    averageRate = cmsFractionValue;
            }
            if (arg.startsWith(useGCFraction)) {
                useG1GC = true;
            }
        }

        double criticalRate = averageRate + 2 * safeMem;
        final double adjustLRU = 1.0 + safeMem;

        final long maxMem = tenuredGenPool.getUsage().getMax();
        final double criticalMem = maxMem * criticalRate;
        final double averageMem = maxMem * averageRate;
        final double upAverageMem = averageMem * (1.0 + safeMem);
        final double downAverageMem = averageMem * (1.0 - safeMem);

        final boolean concurrent = !tenuredGenPool.isCollectionUsageThresholdSupported() || useG1GC;
        final long longCriticalMem = (long) Math.floor(criticalMem);

        scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("lru-tuner"));

        if(concurrent)
            tenuredGenPool.setUsageThreshold(longCriticalMem);
        else
            tenuredGenPool.setCollectionUsageThreshold(longCriticalMem);

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter) mbean;
        emitter.addNotificationListener(new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {
                if(concurrent) {
                    if (n.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                        synchronized (lockCleanLRU) {
                            if(!runningCleanLRU) {
                                runningCleanLRU = true;
                                logger.log("MEMORY THRESHOLD EXCEEDED");
                                
                                scheduler.schedule(new Runnable() {
                                    public void run() {
                                        assert runningCleanLRU;
                                        long used = tenuredGenPool.getUsage().getUsed();
                                        logger.log("MEMORY THRESHOLD EXCEEDED, USED : " + used + ", CRITICAL : " + longCriticalMem);
                                        if(used > longCriticalMem) { // если все еще used вырезаем еще
                                            double cleanMem = 1.0 - (averageMem / used);
                                            logger.log("REMOVED " + cleanMem + " / RESCHEDULE " + " " + (long) (100 * cleanMem / memGCIn100Millis));
//                                            System.out.println("REMOVED / RESCHEDULE " + cleanMem + " " + (long) (100 * cleanMem / memGCIn100Millis));

                                            ALRUMap.forceRemoveAllLRU(cleanMem);
                                            scheduler.schedule(this, (long) (100 * cleanMem / memGCIn100Millis), TimeUnit.MILLISECONDS);
                                        } else
                                            runningCleanLRU = false;
                                    }
                                }, 0, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                } else {
                    if (n.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                        long used = tenuredGenPool.getCollectionUsage().getUsed();
                        double cleanMem = 1.0 - (averageMem / used);
                        logger.log("MEMORY COLLECTION THRESHOLD EXCEEDED, USED : " + used + ", CLEAN : " + cleanMem);
                        ALRUMap.forceRemoveAllLRU(cleanMem);
                    }
                }
            }}, null, null);

        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {

                long used;
                if(concurrent)
                    used = tenuredGenPool.getUsage().getUsed();
                else
                    used = tenuredGenPool.getCollectionUsage().getUsed();
                
                if(used!=lastCollected) { // прошла сборка мусора
                    logger.log("COLLECTED, USED : " + used + ", LASTCOLLECTED : " + lastCollected + ", UPAVERAGE : " + upAverageMem + ", DOWNAVERAGE : " + downAverageMem);
                    if (used > lastCollected && used > upAverageMem) { // память растет и мы ниже критического предела, ускоряем сборку LRU
                        multiplier /= adjustLRU;
                        logger.log("DEC MULTI " + multiplier);
                    }
                    if (used < lastCollected && used < downAverageMem && multiplier < MAX_MULTIPLIER) { // память уменьшается и мы выше критического предела, замедляем сборку LRU
                        multiplier *= adjustLRU;
                        logger.log("INC MULTI " + multiplier);
                    }

                    lastCollected = used;
                }
            }
        }, 0, (long) (100 * (adjustLRU - 1.0) / memGCIn100Millis), TimeUnit.MILLISECONDS);
    }

    public enum Value {NULL}

    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    public static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The default concurrency level for this table, used when not
     * otherwise specified in a constructor.
     */
    public static final int DEFAULT_CONCURRENCY_LEVEL = SystemUtils.getAvailableProcessors() * 8;

    public static class Strategy {
        public int baseTime;
        public int maxTime;

        public Strategy(int baseTime, int maxTime) {
            this.baseTime = baseTime;
            this.maxTime = maxTime;
        }

        public Strategy(int baseTime) {
            this(baseTime, Integer.MAX_VALUE);
        }

        public double getExpireTime() {
            return BaseUtils.min(baseTime * multiplier, maxTime);
        }
    }    
    
    public static Strategy L1 = new Strategy(5, 20); // мусор, будем очищать в любом случае, даже если памяти завались, чтобы не насиловать сборщик мусора
    public static Strategy L2 = new Strategy(20, 2 * 60); // может быть полезно, но маловероятно 
    public static Strategy G1 = new Strategy(3 * 60, 30 * 60); // надо иногда, но очищать 
    public static Strategy G2 = new Strategy(60 * 60);
}
