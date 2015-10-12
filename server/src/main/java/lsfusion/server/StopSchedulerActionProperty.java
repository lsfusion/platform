package lsfusion.server;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.Scheduler;
import lsfusion.server.logics.SchedulerLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class StopSchedulerActionProperty extends ScriptingActionProperty {

    public StopSchedulerActionProperty(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getLogicsInstance().getCustomObject(Scheduler.class).stopScheduledTasks();
        try {
            findProperty("isStartedScheduler").change((Object) null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
