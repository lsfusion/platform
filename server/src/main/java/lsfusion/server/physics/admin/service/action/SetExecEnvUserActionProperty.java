package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.query.exec.DynamicExecuteEnvironment;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExecEnvUserActionProperty extends InternalAction {

    public SetExecEnvUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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
