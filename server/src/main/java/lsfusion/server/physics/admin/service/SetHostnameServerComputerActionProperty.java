package lsfusion.server.physics.admin.service;

import lsfusion.server.ServerLoggers;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.exec.DBManager;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

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
