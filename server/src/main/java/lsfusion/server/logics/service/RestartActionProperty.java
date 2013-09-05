package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class RestartActionProperty extends ScriptingActionProperty {
    public RestartActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getRestartManager().scheduleRestart();
        context.getBL().LM.getFormRefresh().execute(context);
    }
}