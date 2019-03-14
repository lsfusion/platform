package lsfusion.server.physics.admin.scheduler;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;

import java.sql.SQLException;

public class StopSchedulerActionProperty extends ScriptingActionProperty {

    public StopSchedulerActionProperty(SchedulerLogicsModule LM) {
        super(LM);

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getLogicsInstance().getCustomObject(Scheduler.class).stopScheduledTasks();
        try {
            findProperty("isStartedScheduler[]").change((Boolean) null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
