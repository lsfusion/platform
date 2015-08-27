package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
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

public class SetThreadAllocatedMemoryEnabledActionProperty extends ScriptingActionProperty {

    public SetThreadAllocatedMemoryEnabledActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            final boolean readAllocatedBytes = findProperty("readAllocatedBytes").read(context) != null;
            Timer timer = new Timer("ReadAllocatedBytes", true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
                    setThreadAllocatedMemoryEnabled(tBean, readAllocatedBytes);
                }
            }, 0, 1800000); //every 30 minutes
        } catch (ScriptingErrorLog.SemanticErrorException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private void setThreadAllocatedMemoryEnabled(ThreadMXBean tBean, boolean readAllocatedBytes) {
        if (tBean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) tBean;
            if (sunBean.isThreadAllocatedMemorySupported()) {
                if (readAllocatedBytes && sunBean.isThreadAllocatedMemoryEnabled()) //reset value
                    sunBean.setThreadAllocatedMemoryEnabled(false);
                sunBean.setThreadAllocatedMemoryEnabled(readAllocatedBytes);
            }
        }
    }
}