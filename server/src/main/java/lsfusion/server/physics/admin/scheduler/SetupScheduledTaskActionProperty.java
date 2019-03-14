package lsfusion.server.physics.admin.scheduler;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Iterator;

public class SetupScheduledTaskActionProperty extends ScriptingAction {
    private final ClassPropertyInterface scheduledTask;

    public SetupScheduledTaskActionProperty(SchedulerLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        scheduledTask = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            boolean isServer = context.getDbManager().isServer();
            if (isServer) {
                DataObject scheduledTaskObject = context.getDataKeyValue(scheduledTask);
                String nameScheduledTask = (String) context.getBL().schedulerLM.nameScheduledTask.read(context, scheduledTaskObject);
                ServerLoggers.startLogger.info("Setup Scheduled Task: " + nameScheduledTask);
                Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
                scheduler.setupScheduledTask(context.getSession(), scheduledTaskObject, nameScheduledTask);

            } else {
                context.delayUserInteraction(new MessageClientAction("Scheduler disabled, change serverComputer() to enable", "Scheduler disabled"));
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}