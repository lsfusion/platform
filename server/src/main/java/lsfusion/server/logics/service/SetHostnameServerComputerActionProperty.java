package lsfusion.server.logics.service;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class SetHostnameServerComputerActionProperty extends ScriptingActionProperty {

    public SetHostnameServerComputerActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object value = context.getSingleKeyObject();
        DBManager.HOSTNAME_COMPUTER = (String) value;
        ServerLoggers.systemLogger.info("Hostname: " + value);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
