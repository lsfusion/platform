package lsfusion.server.caches;

import lsfusion.base.col.MapFact;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class CacheStats {
    public static boolean readCacheStats = true;
    
    private static ConcurrentHashMap<Long, HashMap<CacheType, Long>> cacheMissedStats = MapFact.getGlobalConcurrentHashMap();
    private static ConcurrentHashMap<Long, HashMap<CacheType, Long>> cacheHitStats = MapFact.getGlobalConcurrentHashMap();

    public static void incrementMissed(CacheType type) {
        incrementStats(type, cacheMissedStats);
    }

    public static void incrementHit(CacheType type) {
        incrementStats(type, cacheHitStats);
    }
    
    private static void incrementStats(CacheType type, ConcurrentHashMap<Long, HashMap<CacheType, Long>> cacheStatsMap) {
        if (readCacheStats) {
            long threadId = Thread.currentThread().getId();
            HashMap<CacheType, Long> statsMap = cacheStatsMap.get(threadId);
            if (statsMap == null) {
                statsMap = new HashMap<>();
                cacheStatsMap.put(threadId, statsMap);
            }
            Long stats = statsMap.get(type);
            if (stats == null) {
                stats = 0L;
            }
            statsMap.put(type, stats + 1);
        }
    }
    
    public static ConcurrentHashMap<Long, HashMap<CacheType, Long>> getCacheMissedStats() {
        return cacheMissedStats;
    }
    
    public static ConcurrentHashMap<Long, HashMap<CacheType, Long>> getCacheHitStats() {
        return cacheHitStats;
    }
    
    public static void resetStats() {
        cacheHitStats.clear();
        cacheMissedStats.clear();
    }

    public enum CacheType {
        JOIN, DATA_CHANGES, EXPR, JOIN_EXPR, INCREMENT_CHANGE, READ_SAVE, AUTOHINT, QUERY,
        TWIN_LAZY, USED_CHANGES, PARAM_LAZY, IDENTITY_LAZY, QUICK_LAZY, OTHER;

        @Override
        public String toString() {
            switch (this) {
                case JOIN: 
                    return "J";
                case DATA_CHANGES:
                    return "DC";
                case EXPR:
                    return "E";
                case JOIN_EXPR:
                    return "JE";
                case INCREMENT_CHANGE:
                    return "IC";
                case READ_SAVE:
                    return "RS";
                case AUTOHINT:
                    return "AH";
                case QUERY:
                    return "Q";
                case TWIN_LAZY:
                    return "TL";
                case USED_CHANGES:
                    return "UC";
                case PARAM_LAZY:
                    return "PL";
                case IDENTITY_LAZY:
                    return "IL";
                case QUICK_LAZY:
                    return "QL";
                case OTHER:
                    return "O";
                default:
                    return super.toString();
            }
        }
    }
}
