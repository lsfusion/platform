package lsfusion.server.physics.admin.scheduler.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.scheduler.SchedulerLogicsModule;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class ExecuteScheduledTaskAction extends InternalAction {
    private final ClassPropertyInterface scheduledTask;

    public ExecuteScheduledTaskAction(SchedulerLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        scheduledTask = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject scheduledTaskObject = context.getDataKeyValue(scheduledTask);
        String nameScheduledTask = (String) context.getBL().schedulerLM.nameScheduledTask.read(context, scheduledTaskObject);
        ServerLoggers.startLogger.info("Execute Scheduled Task: " + nameScheduledTask);
        Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
        if(!scheduler.executeScheduledTask(context.getSession(), scheduledTaskObject, nameScheduledTask))
            context.delayUserInteraction(new MessageClientAction("Scheduler disabled, change serverComputer() to enable", "Scheduler disabled"));
    }
}
