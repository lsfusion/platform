package lsfusion.server.logics.property.actions;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;

public class ExecuteLocalEventsActionProperty extends ScriptingActionProperty {

    public ExecuteLocalEventsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getSession().executeSessionEvents(context.getFormInstance());
    }
}
