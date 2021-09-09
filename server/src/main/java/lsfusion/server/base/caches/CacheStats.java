package lsfusion.server.base.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.base.BaseUtils.nullToZero;

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
            if (stats < Long.MAX_VALUE) {
                statsMap.put(type, stats + 1);
            }
        }
    }

    public static Map<CacheType, Long> getCacheHitStats(long threadId) {
        HashMap<CacheType, Long> map = cacheHitStats.get(threadId);
        if(map == null)
            return Collections.emptyMap();
        return new HashMap<>(map);
    }

    public static Map<CacheType, Long> getCacheMissedStats(long threadId) {
        HashMap<CacheType, Long> map = cacheMissedStats.get(threadId);
        if(map == null)
            return Collections.emptyMap();
        return new HashMap<>(map);
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

    public static <T> String getAbsoluteString(Map<T, Long> hitStats, Map<T, Long> missedStats, T[] values) {
        String result = "";
        for (int i = 0; i < values.length; i++) {
            T type = values[i];
            result += type + ": " + nullToZero(missedStats.get(type)) + "-" + nullToZero(hitStats.get(type));
            if (i < values.length - 1) {
                result += "; ";
            }
        }
        return result;
    }
    public static <T> String getRatioString(Map<T, Long> hitStats, Map<T, Long> missedStats, T[] values) {
        String result = "";
        for (int i = 0; i < values.length; i++) {
            T type = values[i];
            long missedCount = nullToZero(missedStats.get(type));
            long hitCount = nullToZero(hitStats.get(type));
            long totalCount = missedCount + hitCount;
            result += type + ": " + (totalCount > 0 ? hitCount * 100 / totalCount : "--") + "% (" + totalCount + ")";
            if (i < values.length - 1) {
                result += "; ";
            }
        }
        return result;
    }
    public static String getAbsoluteString(Map<CacheType, Long> hitStats, Map<CacheType, Long> missedStats) {
        return getAbsoluteString(hitStats, missedStats, CacheType.values());
    }
    public static String getRatioString(Map<CacheType, Long> hitStats, Map<CacheType, Long> missedStats) {
        return getRatioString(hitStats, missedStats, CacheType.values());
    }

    public static String getGroupedRatioString(Map<CacheType, Long> hitStats, Map<CacheType, Long> missedStats) {
        BaseUtils.Group<GroupCacheType, CacheType> groupStats = key -> key.getGroup();
        Map<GroupCacheType, Long> hitGroupStats = BaseUtils.groupSum(groupStats, hitStats);
        Map<GroupCacheType, Long> missedGroupStats = BaseUtils.groupSum(groupStats, missedStats);
        return getRatioString(hitGroupStats, missedGroupStats, GroupCacheType.values()) + " [" + getRatioString(hitStats, missedStats) + "]";
    }

    public static Map<CacheType, Long> diff(Map<CacheType, Long> a, Map<CacheType, Long> b) {
        Map<CacheType, Long> result = new HashMap<>();
        for(CacheType type : CacheType.values())
            result.put(type, nullToZero(a.get(type)) - nullToZero(b.get(type)));
        return result;
    }

    public enum GroupCacheType {
        PRECOMPILE, // top most caches hit/rate can be pretty low (however consumes not that much app time)
        COMPILE, // deep expr caches (query compilation) - the most app consuming
        LAZY, // all lazy caches - used in compilation and outside, so shouldn't be too high if deep expr caches are not very high
        OTHER;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public enum CacheType {
        JOIN, DATA_CHANGES, EXPR, JOIN_EXPR, INCREMENT_CHANGE, READ_SAVE, AUTOHINT, QUERY, TEMP_TABLE,
        TWIN_LAZY, USED_CHANGES, HAS_PREREAD, PARAM_LAZY, PARAM_INSTANCE_LAZY, IDENTITY_LAZY, QUICK_LAZY, INSTANCE_LAZY;

        public GroupCacheType getGroup() {
            switch (this) {
                case USED_CHANGES:
                case AUTOHINT:
                case HAS_PREREAD:
                    return GroupCacheType.PRECOMPILE;
                case JOIN:
                case DATA_CHANGES:
                case EXPR:
                case JOIN_EXPR:
                case INCREMENT_CHANGE:
                case READ_SAVE:
                case QUERY:
                    return GroupCacheType.COMPILE;
                case TWIN_LAZY:
                case PARAM_LAZY:
                case PARAM_INSTANCE_LAZY:
                case IDENTITY_LAZY:
                case QUICK_LAZY:
                case INSTANCE_LAZY:
                    return GroupCacheType.LAZY;
                default:
                    return GroupCacheType.OTHER;
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case TEMP_TABLE:
                    return "TT";
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
                case PARAM_INSTANCE_LAZY:
                    return "PN";
                case IDENTITY_LAZY:
                    return "IL";
                case QUICK_LAZY:
                    return "QL";
                case INSTANCE_LAZY:
                    return "NL";
                case HAS_PREREAD:
                    return "HP";
                default:
                    return super.toString();
            }
        }
    }
}
