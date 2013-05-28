package platform.server.logics.service;

import platform.server.classes.ValueClass;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class ToggleExplainModeActionProperty extends ScriptingActionProperty {

    public ToggleExplainModeActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getSession().sql.toggleExplainMode();
    }
}
