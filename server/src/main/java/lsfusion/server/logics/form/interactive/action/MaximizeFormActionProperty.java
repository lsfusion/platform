package lsfusion.server.logics.form.interactive.action;

import lsfusion.interop.action.MaximizeFormClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

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