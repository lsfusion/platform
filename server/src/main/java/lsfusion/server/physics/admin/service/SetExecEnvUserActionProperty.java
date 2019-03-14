package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExecEnvUserActionProperty extends ScriptingAction {

    public SetExecEnvUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        DynamicExecuteEnvironment.setUserExecEnv((Long) params.get(1), (Integer)params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

}
