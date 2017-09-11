package lsfusion.server.logics.service;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetPausableLogUserActionProperty extends ScriptingActionProperty {

    public SetPausableLogUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        ServerLoggers.setPausableLog((Integer) params.get(1), (Boolean) params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
