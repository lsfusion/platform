package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.RecalculateAggregationsTask;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateMultiThreadActionProperty extends MultiThreadActionProperty {

    public RecalculateMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        super.executeCustom(context);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new RecalculateAggregationsTask();
    }

    @Override
    protected String getCaptionError() {
        return getString("logics.recalculation.aggregations.error");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(getString(errorOccurred ? "logics.recalculation.failed" : "logics.recalculation.completed",
                getString("logics.recalculation.aggregations")) + task.getMessages(), getString("logics.recalculation.aggregations"));
    }
}