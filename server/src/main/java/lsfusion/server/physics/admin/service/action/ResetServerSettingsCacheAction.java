package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.ResetServerSettingsCacheClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ResetServerSettingsCacheAction extends InternalAction {

    public ResetServerSettingsCacheAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.delayUserInteraction(new ResetServerSettingsCacheClientAction());
    }
}
