package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.logics.tasks.impl.RecalculateAggregationsTask;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateMultiThreadActionProperty extends ScriptingActionProperty {
    public RecalculateMultiThreadActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            RecalculateAggregationsTask task = new RecalculateAggregationsTask();
            task.init(context.getBL());
            TaskRunner.runTask(task, ServerLoggers.serviceLogger);

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        } catch (InterruptedException e) {
            ServerLoggers.serviceLogger.error("RecalculateAggregations error", e);
            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), getString("logics.recalculation.aggregations.error")));
        }
    }
}