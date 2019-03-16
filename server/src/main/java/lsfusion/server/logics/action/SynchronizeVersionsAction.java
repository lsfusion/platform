package lsfusion.server.logics.action;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;

import java.sql.SQLException;

public class SynchronizeVersionsAction extends ScriptingAction {

    public SynchronizeVersionsAction(BaseLogicsModule LM) {
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