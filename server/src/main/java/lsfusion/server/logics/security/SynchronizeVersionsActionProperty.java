package lsfusion.server.logics.security;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class SynchronizeVersionsActionProperty extends ScriptingActionProperty {

    public SynchronizeVersionsActionProperty(SecurityLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            findProperty("platformVersion[]").change(BaseUtils.getPlatformVersion(), context);
            findProperty("apiVersion[]").change(BaseUtils.getApiVersion(), context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();
        }
    }
}