package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
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

                        timer = new Timer("ReadAllocatedBytes", true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
                                updateThreadAllocatedBytesMap(tBean);
                            }
                        }, 0, (period <= 0 ? 1800000 : period) / 2);
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private void updateThreadAllocatedBytesMap(ThreadMXBean tBean) {
        if (tBean instanceof com.sun.management.ThreadMXBean && ((com.sun.management.ThreadMXBean) tBean).isThreadAllocatedMemorySupported())
            SQLSession.updateThreadAllocatedBytesMap();
    }
}