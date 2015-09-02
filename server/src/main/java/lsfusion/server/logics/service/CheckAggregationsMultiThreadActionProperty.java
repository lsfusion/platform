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
import lsfusion.server.logics.tasks.impl.CheckAggregationsTask;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CheckAggregationsMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;

    public CheckAggregationsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();

    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner();
        try {
            Integer threadCount = (Integer) context.getKeyValue(threadCountInterface).getValue();
            CheckAggregationsTask task = new CheckAggregationsTask();
            task.init(context);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount);
            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.completed", getString("logics.checking.aggregations")) + task.getMessages(), getString("logics.checking.aggregations")));
        } catch (InterruptedException e) {
            taskRunner.shutdownNow();
            ServerLoggers.serviceLogger.error("Check Aggregations error", e);
            Thread.currentThread().interrupt();
        }
    }
}