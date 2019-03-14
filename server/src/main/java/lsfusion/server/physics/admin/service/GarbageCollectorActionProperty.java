package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;

import java.sql.SQLException;

public class GarbageCollectorActionProperty extends ScriptingAction {
    public GarbageCollectorActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        System.runFinalization();
        System.gc();
    }

}