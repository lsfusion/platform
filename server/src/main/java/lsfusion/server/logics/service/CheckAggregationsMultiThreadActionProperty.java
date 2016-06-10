package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.CheckAggregationsTask;
import lsfusion.server.logics.tasks.impl.recalculate.OverCalculateStatsTask;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CheckAggregationsMultiThreadActionProperty extends MultiThreadActionProperty {

    public CheckAggregationsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new CheckAggregationsTask();
    }

    @Override
    protected String getCaptionError() {
        return getString("logics.check.aggregation.error");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(getString(errorOccurred ? "logics.check.failed" : "logics.check.completed",
                getString("logics.checking.aggregations")) + task.getMessages(), getString("logics.checking.aggregations"));
    }
}