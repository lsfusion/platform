package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class SynchronizeVersionsActionProperty extends ScriptingAction {

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