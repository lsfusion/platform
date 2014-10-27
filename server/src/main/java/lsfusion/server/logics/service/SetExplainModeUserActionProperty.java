package lsfusion.server.logics.service;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExplainModeUserActionProperty extends ScriptingActionProperty {

    public SetExplainModeUserActionProperty(ServiceLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LogicalClass.instance, LM.findClass("User"));
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<Object>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }

        context.getSession().sql.setExplainMode((Integer) params.get(1), (Boolean) params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
