package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetVolatileStatsEnabledUserActionProperty extends ScriptingActionProperty {

    public SetVolatileStatsEnabledUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        List<Object> params = new ArrayList<Object>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        Boolean enabled = (Boolean) params.get(0);
        SQLSession.setVolatileStats((Integer) params.get(1), enabled != null && enabled, session.getOwner());
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
