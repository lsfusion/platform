package lsfusion.server.logics.navigator;

import lsfusion.interop.action.ResetWindowsLayoutClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ResetWindowsLayoutAction extends InternalAction {
    public ResetWindowsLayoutAction(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new ResetWindowsLayoutClientAction());
    }
}
