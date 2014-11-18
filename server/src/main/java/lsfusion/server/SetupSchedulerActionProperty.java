package lsfusion.server;

import lsfusion.server.classes.ValueClass;
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
        Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
        try {
            scheduler.setupScheduledTasks(context.getSession());
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException("Error starting Scheduler: ", e);
        }
    }
}
