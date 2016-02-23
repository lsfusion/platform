package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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

                        final LogicsInstance logicsInstance = context.getLogicsInstance();
                        timer = new Timer("ReadAllocatedBytes", true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ThreadLocalContext.set(logicsInstance.getContext());
                                ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
                                updateThreadAllocatedBytesMap(tBean, maxAllocatedBytes);
                            }
                        }, 0, (period <= 0 ? 1800000 : period) / 2);
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private void updateThreadAllocatedBytesMap(ThreadMXBean tBean, long maxAllocatedBytes) {
        if (tBean instanceof com.sun.management.ThreadMXBean && ((com.sun.management.ThreadMXBean) tBean).isThreadAllocatedMemorySupported()) {
            long time = System.currentTimeMillis();
            long sum = 0;
            long totalSum = 0;
            SQLSession.updateThreadAllocatedBytesMap();
            Map<Long, Thread> threadMap = ThreadUtils.getThreadMap();

            for (Map.Entry<Long, Long> bEntry : SQLSession.threadAllocatedBytesBMap.entrySet()) {
                Long id = bEntry.getKey();
                if (id != null) {
                    Long bBytes = bEntry.getValue();
                    Long aBytes = SQLSession.threadAllocatedBytesAMap.get(bEntry.getKey());

                    Long delta = bBytes != null && aBytes != null ? (bBytes - aBytes) : 0;
                    totalSum += delta;
                    if (delta > maxAllocatedBytes) {
                        sum += delta;
                        Thread thread = threadMap.get(id);
                        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
                        String computer = logInfo == null ? null : logInfo.hostnameComputer;
                        String user = logInfo == null ? null : logInfo.userName;
                        if (user == null)
                            ServerLoggers.allocatedBytesLogger.info(String.format("Process ID %s: %s", bEntry.getKey(), humanReadableByteCount(delta)));
                        else
                            ServerLoggers.allocatedBytesLogger.info(String.format("Process ID %s, Computer %s, User %s: %s", bEntry.getKey(),
                                    computer == null ? "unknown" : computer, user, humanReadableByteCount(delta)));
                    }
                }
            }
            if (sum > 0)
                ServerLoggers.allocatedBytesLogger.info(String.format("Reading allocated bytes: elapsed %sms, sum: %s, totalSum: %s",
                        System.currentTimeMillis() - time, humanReadableByteCount(sum), humanReadableByteCount(totalSum)));
        }
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}