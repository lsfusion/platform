package lsfusion.server.physics.admin.service;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class GetVMInfoActionProperty extends ScriptingAction {
    public GetVMInfoActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String message = SystemUtils.getVMInfo();
        context.delayUserInterfaction(new MessageClientAction(message, ThreadLocalContext.localize("{vm.data}")));
    }
}