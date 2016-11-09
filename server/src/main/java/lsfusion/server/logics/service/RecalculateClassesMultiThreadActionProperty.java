package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.RecalculateClassesTask;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateClassesMultiThreadActionProperty extends MultiThreadActionProperty {

    public RecalculateClassesMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new RecalculateClassesTask();
    }

    @Override
    protected String getCaptionError() {
        return getString("logics.recalculation.classes.error");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(getString(errorOccurred ? "logics.recalculation.failed" : "logics.recalculation.completed",
                getString("logics.recalculating.data.classes")) + task.getMessages(), getString("logics.recalculating.data.classes"));
    }
}