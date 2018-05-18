package lsfusion.server.logics.security;

import lsfusion.interop.action.MaximizeFormClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class MaximizeFormActionProperty extends ScriptingActionProperty {

    public MaximizeFormActionProperty(SecurityLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new MaximizeFormClientAction());
    }
}