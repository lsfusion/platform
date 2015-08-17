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

public class SetThreadAllocatedMemoryEnabledActionProperty extends ScriptingActionProperty {

    public SetThreadAllocatedMemoryEnabledActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = findProperty("readAllocatedBytes").read(context) != null;
            ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
            setThreadAllocatedMemoryEnabled(tBean, readAllocatedBytes);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    private void setThreadAllocatedMemoryEnabled(ThreadMXBean tBean, boolean readAllocatedBytes) {
        if (tBean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) tBean;
            if (sunBean.isThreadAllocatedMemorySupported()) {
                sunBean.setThreadAllocatedMemoryEnabled(readAllocatedBytes);
            }
        }
    }
}