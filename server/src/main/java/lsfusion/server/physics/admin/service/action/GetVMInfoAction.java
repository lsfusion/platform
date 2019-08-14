package lsfusion.server.physics.admin.service.action;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class GetVMInfoAction extends InternalAction {
    public GetVMInfoAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        String message = SystemUtils.getVMInfo();
        context.delayUserInterfaction(new MessageClientAction(message, ThreadLocalContext.localize("{vm.data}")));
    }
}