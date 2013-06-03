package lsfusion.server.logics.service;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

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