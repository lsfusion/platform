package lsfusion.server;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.Scheduler;
import lsfusion.server.logics.SchedulerLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class SetupSchedulerActionProperty extends ScriptingActionProperty {

    public SetupSchedulerActionProperty(SchedulerLogicsModule LM) {
        super(LM, new ValueClass[]{});

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        Scheduler scheduler = context.getLogicsInstance().getCustomObject(Scheduler.class);
        scheduler.setupScheduledTasks(context.getSession());
    }
}
