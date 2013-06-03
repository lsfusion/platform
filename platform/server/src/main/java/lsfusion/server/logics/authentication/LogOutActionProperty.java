package lsfusion.server.logics.authentication;

import lsfusion.interop.action.LogOutClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class LogOutActionProperty extends ScriptingActionProperty {
    public LogOutActionProperty(AuthenticationLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInteraction(new LogOutClientAction());
    }
}
