package lsfusion.server.physics.admin.service;

import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExLogUserActionProperty extends ScriptingActionProperty {

    public SetExLogUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        ServerLoggers.setUserExLog((Long) params.get(1), (Boolean) params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
