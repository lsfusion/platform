package platform.server;

import platform.server.classes.ValueClass;
import platform.server.logics.Scheduler;
import platform.server.logics.SchedulerLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

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
