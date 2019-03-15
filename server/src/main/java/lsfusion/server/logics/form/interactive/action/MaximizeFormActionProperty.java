package lsfusion.server.logics.form.interactive.action;

import lsfusion.interop.action.MaximizeFormClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;

import java.sql.SQLException;

public class MaximizeFormActionProperty extends ScriptingAction {

    public MaximizeFormActionProperty(SecurityLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new MaximizeFormClientAction());
    }
}