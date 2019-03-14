package lsfusion.server.physics.admin.service;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetLoggerDebugEnabledUserActionProperty extends ScriptingAction {

    public SetLoggerDebugEnabledUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        SQLSession.setLoggerDebugEnabled((Long) params.get(1), (Boolean) params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
