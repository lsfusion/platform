package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.form.navigator.LogInfo;
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

                        timer = new Timer("ReadAllocatedBytes", true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
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

            SQLSession.updateThreadAllocatedBytesMap();

            boolean found = false;
            for (Map.Entry<Long, Long> bEntry : SQLSession.threadAllocatedBytesBMap.entrySet()) {
                Long id = bEntry.getKey();
                if (id != null) {
                    Long bBytes = bEntry.getValue();
                    Long aBytes = SQLSession.threadAllocatedBytesAMap.get(bEntry.getKey());

                    Long delta = bBytes != null && aBytes != null ? (bBytes - aBytes) : 0;
                    if (delta > maxAllocatedBytes) {
                        found = true;
                        Thread thread = getThreadById(id.intValue());
                        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
                        String computer = logInfo == null ? null : logInfo.hostnameComputer;
                        String user = logInfo == null ? null : logInfo.userName;
                        if (user == null)
                            ServerLoggers.allocatedBytesLogger.info(String.format("Process ID %s: %s bytes", bEntry.getKey(), delta));
                        else
                            ServerLoggers.allocatedBytesLogger.info(String.format("Process ID %s, Computer %s, User %s: %s bytes", bEntry.getKey(),
                                    computer == null ? "unknown" : computer, user, delta));
                    }
                }
            }
            if (found)
                ServerLoggers.allocatedBytesLogger.info(String.format("Reading allocated bytes: elapsed %sms", System.currentTimeMillis() - time));
        }
    }

    private Thread getThreadById(int id) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }
}