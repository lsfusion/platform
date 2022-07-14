package lsfusion.server.physics.admin.scheduler.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.scheduler.SchedulerLogicsModule;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class SetupSchedulerAction extends InternalAction {

    public SetupSchedulerAction(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ServerLoggers.startLogger.info("Starting Scheduler");
            Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
            Integer threadCount = (Integer) findProperty("threadCountScheduler[]").read(context);
            boolean onlySystemTasks = scheduler.setupScheduledTasks(context.getSession(), threadCount);
            if(onlySystemTasks)
                context.delayUserInteraction(new MessageClientAction("Scheduler will execute only system tasks, change serverComputer() to enable user tasks", "Scheduler disabled"));
            findProperty("isStartedScheduler[]").change(true, context);
            findProperty("onlySystemTasks[]").change(onlySystemTasks, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
