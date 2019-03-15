package lsfusion.server.physics.admin.scheduler;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class SetupSchedulerActionProperty extends ScriptingAction {

    public SetupSchedulerActionProperty(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            boolean isServer = context.getDbManager().isServer();
            if (isServer) {
                ServerLoggers.startLogger.info("Starting Scheduler");
                Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
                Integer threadCount = (Integer) findProperty("threadCountScheduler[]").read(context);
                scheduler.setupScheduledTasks(context.getSession(), threadCount);
            } else {
                context.delayUserInteraction(new MessageClientAction("Scheduler disabled, change serverComputer() to enable", "Scheduler disabled"));
            }
            findProperty("isStartedScheduler[]").change(isServer ? true : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
