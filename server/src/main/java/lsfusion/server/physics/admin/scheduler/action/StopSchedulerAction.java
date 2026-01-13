package lsfusion.server.physics.admin.scheduler.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.scheduler.SchedulerLogicsModule;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class StopSchedulerAction extends InternalAction {

    public StopSchedulerAction(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getLogicsInstance().getCustomObject(Scheduler.class).stopScheduledTasks(context);
        try {
            findProperty("isStartedScheduler[]").change((Boolean) null, context);
            findProperty("onlySystemTasks[]").change((Boolean) null, context);
            String currentUser = (String) findProperty("currentUserLogin[]").read(context);
            ServerLoggers.schedulerLogger.warn("Scheduler is stopped by " + currentUser);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
