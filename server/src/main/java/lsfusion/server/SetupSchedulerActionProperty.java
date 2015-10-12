package lsfusion.server;

import com.google.common.base.Throwables;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.Scheduler;
import lsfusion.server.logics.SchedulerLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class SetupSchedulerActionProperty extends ScriptingActionProperty {

    public SetupSchedulerActionProperty(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            boolean isServer = context.getDbManager().isServer();
            if (isServer) {
                ServerLoggers.systemLogger.info("Starting Scheduler");
                Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
                scheduler.setupScheduledTasks(context.getSession());
            } else {
                context.delayUserInteraction(new MessageClientAction("Scheduler disabled, change serverComputer() to enable", "Scheduler disabled"));
            }
            findProperty("isStartedScheduler").change(isServer ? true : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
