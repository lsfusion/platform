package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;

import java.sql.SQLException;

public class SynchronizeVersionsActionProperty extends ScriptingActionProperty {

    public SynchronizeVersionsActionProperty(BaseLogicsModule LM) {
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