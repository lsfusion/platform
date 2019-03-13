package lsfusion.server.physics.admin.service;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetVolatileStatsEnabledUserActionProperty extends ScriptingActionProperty {

    public SetVolatileStatsEnabledUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        Boolean enabled = (Boolean) params.get(0);
        SQLSession.setVolatileStats((Long) params.get(1), enabled != null && enabled, session.getOwner());
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
