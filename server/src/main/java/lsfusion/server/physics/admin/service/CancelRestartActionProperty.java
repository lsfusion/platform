package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class CancelRestartActionProperty extends ScriptingAction {
    public CancelRestartActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getRestartManager().cancelRestart();
    }
}