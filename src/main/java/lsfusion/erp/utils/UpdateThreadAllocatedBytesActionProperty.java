package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.caches.CacheStats;
import lsfusion.server.caches.CacheStats.CacheType;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.base.BaseUtils.nullToZero;

public class UpdateThreadAllocatedBytesActionProperty extends ScriptingActionProperty {
    private static Timer timer;
    public UpdateThreadAllocatedBytesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            synchronized (this) {
                final boolean readAllocatedBytes = findProperty("readAllocatedBytes[]").read(context) != null;
                if(readAllocatedBytes == (timer == null)) {
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    } else {
                        final long period = Settings.get().getThreadAllocatedMemoryPeriod();
                        final long maxAllocatedBytes = Settings.get().getMaxThreadAllocatedBytes();
                        final int cacheMissesStatsLimit = Settings.get().getCacheMissesStatsLimit();

                        final LogicsInstance logicsInstance = context.getLogicsInstance();
                        timer = new Timer("ReadAllocatedBytes", true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ThreadLocalContext.set(logicsInstance.getContext());
                                updateThreadAllocatedBytesMap(maxAllocatedBytes, cacheMissesStatsLimit);
                            }
                        }, 0, (period <= 0 ? 1800000 : period) / 2);
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private void updateThreadAllocatedBytesMap(long maxAllocatedBytes, int cacheMissesStatsLimit) {
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        if (tBean instanceof com.sun.management.ThreadMXBean && ((com.sun.management.ThreadMXBean) tBean).isThreadAllocatedMemorySupported()) {
            long time = System.currentTimeMillis();
            long bytesSum = 0;
            long totalBytesSum = 0;
            SQLSession.updateThreadAllocatedBytesMap();
            Map<Long, Thread> threadMap = ThreadUtils.getThreadMap();

            ConcurrentHashMap<Long, HashMap<CacheType, Long>> hitStats = MapFact.getGlobalConcurrentHashMap(CacheStats.getCacheHitStats());
            ConcurrentHashMap<Long, HashMap<CacheType, Long>> missedStats = MapFact.getGlobalConcurrentHashMap(CacheStats.getCacheMissedStats());
            CacheStats.resetStats();

            long totalHit = 0;
            long totalMissed = 0;
            HashMap<CacheType, Long> totalHitMap = new HashMap<>();
            HashMap<CacheType, Long> totalMissedMap = new HashMap<>();
            long exceededMisses = 0;
            long exceededMissesHits = 0;
            HashMap<CacheType, Long> exceededHitMap = new HashMap<>();
            HashMap<CacheType, Long> exceededMissedMap = new HashMap<>();

            boolean logTotal = false;
            
            for (Map.Entry<Long, Long> bEntry : SQLSession.threadAllocatedBytesBMap.entrySet()) {
                Long id = bEntry.getKey();
                if (id != null) {
                    Long bBytes = bEntry.getValue();
                    Long aBytes = SQLSession.threadAllocatedBytesAMap.get(bEntry.getKey());

                    Long deltaBytes = bBytes != null && aBytes != null ? (bBytes - aBytes) : 0;
                    totalBytesSum += deltaBytes;

                    long userMissed = 0;
                    long userHit = 0;

                    HashMap<CacheType, Long> userHitMap = hitStats.get(id) != null ? hitStats.get(id) : new HashMap<CacheType, Long>();
                    HashMap<CacheType, Long> userMissedMap = missedStats.get(id) != null ? missedStats.get(id) : new HashMap<CacheType, Long>();
                    for (CacheType cacheType : CacheType.values()) {
                        Long hit = nullToZero(userHitMap.get(cacheType));
                        Long missed = nullToZero(userMissedMap.get(cacheType));
                        userHit += hit;
                        userMissed += missed;
                    }
                    totalHit += userHit;
                    totalMissed += userMissed;
                    sumMap(totalHitMap, userHitMap);
                    sumMap(totalMissedMap, userMissedMap);
                    
                    if (deltaBytes > maxAllocatedBytes || userMissed > cacheMissesStatsLimit) {
                        logTotal = true;
                        
                        bytesSum += deltaBytes;

                        exceededMisses += userMissed;
                        exceededMissesHits += userHit;
                        sumMap(exceededHitMap, userHitMap);
                        sumMap(exceededMissedMap, userMissedMap);
                        
                        Thread thread = threadMap.get(id);
                        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
                        String computer = logInfo == null ? null : logInfo.hostnameComputer;
                        String user = logInfo == null ? null : logInfo.userName;
                        
                        String userMessage;
                        if (user == null) {
                            userMessage = String.format("PID %s: %s", bEntry.getKey(), humanReadableByteCount(deltaBytes));
                        } else {
                            userMessage = String.format("PID %s, %s, Comp. %s, User %s", bEntry.getKey(),
                                    humanReadableByteCount(deltaBytes), computer == null ? "unknown" : computer, user);
                        }
                        userMessage += String.format(", missed-hit: All: %s-%s, %s", userMissed, userHit, getString(userHitMap, userMissedMap));
                        
                        ServerLoggers.allocatedBytesLogger.info(userMessage);
                    }
                }
            }
            if (logTotal) {
                ServerLoggers.allocatedBytesLogger.info(String.format("Exceeded: sum: %s, \t\t\tmissed-hit: All: %s-%s, %s", 
                        humanReadableByteCount(bytesSum), exceededMisses, exceededMissesHits, getString(exceededHitMap, exceededMissedMap)));
                ServerLoggers.allocatedBytesLogger.info(String.format("Total: sum: %s, elapsed %sms, missed-hit: All: %s-%s, %s",
                        humanReadableByteCount(totalBytesSum), System.currentTimeMillis() - time, totalMissed, totalHit, getString(totalHitMap, totalMissedMap)));
            }
        }
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    
    private String getString(HashMap<CacheType, Long> hitStats, HashMap<CacheType, Long> missedStats) {
        String result = "";
        for (int i = 0; i < CacheType.values().length; i++) {
            CacheType type = CacheType.values()[i];
            result += type + ": " + nullToZero(missedStats.get(type)) + "-" + nullToZero(hitStats.get(type));
            if (i < CacheType.values().length - 1) {
                result += "; ";
            }
        }
        return result;
    }
    
    private void sumMap(HashMap<CacheType, Long> target, HashMap<CacheType, Long> source) {
        for (CacheType type : CacheType.values()) {
            target.put(type, nullToZero(target.get(type)) + nullToZero(source.get(type)));
        }
    }
}