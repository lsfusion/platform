package platform.server.logics.security;

import platform.interop.action.LogOutClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.SecurityLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class LogOutActionProperty extends ScriptingActionProperty {
    public LogOutActionProperty(SecurityLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInteraction(new LogOutClientAction());
    }
}
