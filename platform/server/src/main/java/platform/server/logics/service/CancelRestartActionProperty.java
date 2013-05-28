package platform.server.logics.service;

import platform.server.classes.ValueClass;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class CancelRestartActionProperty extends ScriptingActionProperty {
    public CancelRestartActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getRestartManager().cancelRestart();
        context.getBL().LM.formRefresh.execute(context);
    }
}