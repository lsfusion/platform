package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.data.query.TypeExecuteEnvironment;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExecEnvUserActionProperty extends ScriptingActionProperty {

    public SetExecEnvUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        DynamicExecuteEnvironment.setUserExecEnv((Integer) params.get(1), (Integer)params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

}
