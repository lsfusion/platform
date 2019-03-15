package lsfusion.server.physics.admin.service;

import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.exec.db.DBManager;

import java.sql.SQLException;

public class SetHostnameServerComputerActionProperty extends ScriptingAction {

    public SetHostnameServerComputerActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object value = context.getSingleKeyObject();
        DBManager.HOSTNAME_COMPUTER = (String) value;
        ServerLoggers.systemLogger.info("Setting hostname: " + value);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
