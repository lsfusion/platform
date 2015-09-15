package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.logics.tasks.impl.recalculate.RecalculateStatsTask;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateStatsMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;
    private ClassPropertyInterface propertyTimeoutInterface;

    public RecalculateStatsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
        propertyTimeoutInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner(context.getBL());
        RecalculateStatsTask task = new RecalculateStatsTask();
        try {
            ObjectValue threadCount = context.getKeyValue(threadCountInterface);
            ObjectValue propertyTimeout = context.getKeyValue(propertyTimeoutInterface);
            task.init(context);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount == null ? null : (Integer) threadCount.getValue(),
                    propertyTimeout == null ? null : (Integer) propertyTimeout.getValue());
        } catch (InterruptedException e) {
            task.logTimeoutTasks();
            taskRunner.shutdownNow();
            ServerLoggers.serviceLogger.error("Recalculate Stats error", e);
            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), getString("logics.recalculation.stats.error")));
            ThreadUtils.interruptThread(context, Thread.currentThread());
            taskRunner.interruptThreadPoolProcesses(context);
        } finally {
            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.completed", getString("logics.recalculation.stats")) + task.getMessages(), getString("logics.recalculation.stats")));
        }
    }

}