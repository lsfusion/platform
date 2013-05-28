package platform.server.logics.service;

import platform.base.SystemUtils;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class GetVMInfoActionProperty extends ScriptingActionProperty {
    public GetVMInfoActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        String message = SystemUtils.getVMInfo();
        context.delayUserInterfaction(new MessageClientAction(message, getString("vm.data")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}