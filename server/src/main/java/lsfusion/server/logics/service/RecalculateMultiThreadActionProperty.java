package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.logics.tasks.impl.RecalculateAggregationsTask;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;
    private ClassPropertyInterface propertyTimeoutInterface;

    public RecalculateMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
        propertyTimeoutInterface = i.next();

    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner(context.getBL());
        RecalculateAggregationsTask task = new RecalculateAggregationsTask();
        try {
            Integer threadCount = (Integer) context.getKeyValue(threadCountInterface).getValue();
            Integer propertyTimeout = (Integer) context.getKeyValue(propertyTimeoutInterface).getValue();
            task.init(context);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount, propertyTimeout);
        } catch (InterruptedException e) {
            task.logTimeoutTasks();
            taskRunner.shutdownNow();
            ServerLoggers.serviceLogger.error("RecalculateAggregations error", e);
            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), getString("logics.recalculation.aggregations.error")));
            Thread.currentThread().interrupt();
            taskRunner.killSQLProcesses();
        } finally {
            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.completed", getString("logics.recalculation.aggregations")) + task.getMessages(), getString("logics.recalculation.aggregations")));
        }
    }
}