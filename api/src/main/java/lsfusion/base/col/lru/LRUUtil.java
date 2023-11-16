package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;
import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.SystemUtils;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LRUUtil {
    private static final int UPDATE_CURTIME_FREQUENCY = 1 << 7;
    static final int UPDATE_CURTIME_FREQUENCY_MASK = UPDATE_CURTIME_FREQUENCY - 1;
    private final static Object lockCleanLRU = new Object();
    
    public static double multiplier = 1.0;
    public static double MAX_MULTIPLIER = 10.0; // чтобы не рос до бесконечности, из - за того что некоторые кэши имеют максимальные пределы
    public static double MIN_MULTIPLIER = 1/MAX_MULTIPLIER; // чтобы не падал до бесконечности, так как в этом случае все равно кэши будут ротироваться с бешеной скоростью

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
    private static long lastStableCollected;
    private static long lastUnadjusted;
    private static boolean needAdjustment;
    private static long lastCriticalMem;
    private static boolean runningCleanLRU = false;

    private static final String cmsFraction = "-XX:CMSInitiatingOccupancyFraction=";
    private static final String newSizeFraction = "-XX:G1NewSizePercent=";
    public static void initLRUTuner(final LRULogger logger, Runnable beforeAspect, Runnable afterAspect, Supplier<Double> targetMemRatio, Supplier<Double> criticalMemRatio, Supplier<Double> adjustTargetIncLRU, Supplier<Double> adjustTargetDecLRU, Supplier<Double> adjustCriticalLRU, double LRUDefCoeff, Supplier<Double> LRUMinCoeff, Supplier<Double> LRUMaxCoeff, Supplier<Long> stableMinCount, Supplier<Long> unstableMaxCount) {
        final MemoryPoolMXBean tenuredGenPool = getTenuredPool();

        // the same as in updateSavePointsInfo
        Double newSizePercent = null;
        Double cmsInitiatingOccupancyFraction = null;
        for(String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith(cmsFraction)) {
                cmsInitiatingOccupancyFraction = readPercentArg(arg);
            } else if (arg.startsWith(newSizeFraction))
                newSizePercent = readPercentArg(arg);
        }

        long oldMem = tenuredGenPool.getUsage().getMax();
        if(newSizePercent != null)
            oldMem = (long) (oldMem - oldMem * newSizePercent);
        else if(cmsInitiatingOccupancyFraction != null) // backward compatibility with cms
            oldMem = (long) (cmsInitiatingOccupancyFraction * oldMem / (1 - 2 * criticalMemRatio.get() - targetMemRatio.get())); 
        long maxMem = oldMem; 
        
        final Supplier<Long> criticalMem = () -> (long) (maxMem - maxMem * criticalMemRatio.get());
        final Supplier<Long> upAverageMem = () -> (long) (criticalMem.get() - maxMem * criticalMemRatio.get());
        final Supplier<Long> averageMem = () -> (long) (upAverageMem.get() - maxMem * targetMemRatio.get());        
        final Supplier<Long> downAverageMem = () -> (long) (averageMem.get() - maxMem * targetMemRatio.get());

        scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("lru-tuner"));
        multiplier = LRUDefCoeff;

        final boolean collectionUsageNotSupported = !tenuredGenPool.isCollectionUsageThresholdSupported(); // it seems that collectionusage for g1 is pretty relevant now (so we don't need to disable it) : https://bugs.openjdk.java.net/browse/JDK-8195115  

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter) mbean;
        emitter.addNotificationListener(new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {
                beforeAspect.run();
                try {
                    if (collectionUsageNotSupported) {
                        if (n.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                            synchronized (lockCleanLRU) {
                                if (!runningCleanLRU) {
                                    runningCleanLRU = true;
                                    logger.log("MEMORY THRESHOLD EXCEEDED");

                                    scheduler.schedule(new Runnable() {
                                        public void run() {
                                            assert runningCleanLRU;
                                            long used = tenuredGenPool.getUsage().getUsed();
                                            Long criticalMemValue = criticalMem.get();
                                            logger.log("MEMORY THRESHOLD EXCEEDED, USED : " + used + ", CRITICAL : " + criticalMemValue);
                                            if (used > criticalMemValue) { // если все еще used вырезаем еще
                                                double cleanMem = 1.0 - ((double) averageMem.get() / (double) used);
                                                logger.log("REMOVED " + cleanMem + " / RESCHEDULE " + " " + (long) (100 * cleanMem / memGCIn100Millis));

                                                if (multiplier > LRUMinCoeff.get()) {
                                                    multiplier /= (1.0 + cleanMem * adjustCriticalLRU.get());
                                                    logger.log("DEC THRESHOLD MULTI " + multiplier);
                                                }
//                                            System.out.println("REMOVED / RESCHEDULE " + cleanMem + " " + (long) (100 * cleanMem / memGCIn100Millis));

                                                ALRUMap.forceRemoveAllLRU(cleanMem);
                                                scheduler.schedule(this, (long) (100 * cleanMem / memGCIn100Millis), TimeUnit.MILLISECONDS);
                                            } else runningCleanLRU = false;
                                        }
                                    }, 0, TimeUnit.MILLISECONDS);
                                }
                            }
                        }
                    } else {
                        if (n.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                            long used = tenuredGenPool.getCollectionUsage().getUsed();
                            double cleanMem = 1.0 - ((double) averageMem.get() / (double) used);
                            logger.log("MEMORY COLLECTION THRESHOLD EXCEEDED, USED : " + used + ", CLEAN : " + cleanMem);

                            if (multiplier > LRUMinCoeff.get()) {
                                multiplier /= (1.0 + cleanMem * adjustCriticalLRU.get());
                                logger.log("DEC THRESHOLD MULTI " + multiplier);
                            }

                            ALRUMap.forceRemoveAllLRU(cleanMem);
                        }
                    }
                } finally {
                    afterAspect.run();
                }
            }}, null, null);

        scheduler.scheduleAtFixedRate(() -> {
            beforeAspect.run();
            try {
                long criticalMemValue = criticalMem.get();
                if (criticalMemValue != lastCriticalMem) {
                    if (collectionUsageNotSupported) 
                        tenuredGenPool.setUsageThreshold(criticalMemValue);
                    else 
                        tenuredGenPool.setCollectionUsageThreshold(criticalMemValue);
                    lastCriticalMem = criticalMemValue;
                }

                long used;
                if (collectionUsageNotSupported) 
                    used = tenuredGenPool.getUsage().getUsed();
                else 
                    used = tenuredGenPool.getCollectionUsage().getUsed();

                if (used != lastCollected) { // g1 when doing mixed garbage collection can do it in several cycles, so we'll wait until collection usage becomes stable
                    logger.log("COLLECTED, USED : " + used + ", LASTCOLLECTED : " + lastCollected);
                    lastCollected = used;

                    needAdjustment = true;
                    lastStableCollected = 0;
                } else
                    lastStableCollected++;

                if(needAdjustment) {
                    if(lastStableCollected > stableMinCount.get() || lastUnadjusted > unstableMaxCount.get()) {
                        long upAverageMemValue = upAverageMem.get();
                        long downAverageMemValue = downAverageMem.get();
                        logger.log("ADJUST COLLECTED : " + lastCollected + " UPAVERAGE : " + upAverageMemValue + ", DOWNAVERAGE : " + downAverageMemValue);
                        if (lastCollected > upAverageMemValue && multiplier > LRUMinCoeff.get()) {
                            multiplier /= (1.0 + targetMemRatio.get() * adjustTargetDecLRU.get());
                            logger.log("DEC MULTI " + multiplier);
                        }
                        if (lastCollected < downAverageMemValue && multiplier < LRUMaxCoeff.get()) {
                            multiplier *= (1.0 + targetMemRatio.get() * adjustTargetIncLRU.get());
                            logger.log("INC MULTI " + multiplier);
                        }
                        needAdjustment = false;
                        lastUnadjusted = 0;
                    } else
                        lastUnadjusted++;
                }
            } finally {
                afterAspect.run();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public static double readPercentArg(String arg) {
        Double cmsFractionValue = null;
        try {
            cmsFractionValue = Double.valueOf(arg.substring(cmsFraction.length())) / 100.0;
        } catch (NumberFormatException e) {
        }
        return cmsFractionValue;
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
    public static final int DEFAULT_CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors() * 8;

    public static class Strategy {
        private final int baseTime;
        private final int maxTime;

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
    public static Strategy G3 = new Strategy(60 * 60 * 24 * 7);
}
