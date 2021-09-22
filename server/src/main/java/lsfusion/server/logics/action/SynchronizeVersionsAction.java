package lsfusion.server.logics.action;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.base.SystemUtils.getRevision;

public class SynchronizeVersionsAction extends InternalAction {

    public SynchronizeVersionsAction(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            findProperty("platformVersion[]").change(BaseUtils.getPlatformVersion(), context);
            findProperty("apiVersion[]").change(BaseUtils.getApiVersion(), context);
            findProperty("revisionVersion[]").change(BaseUtils.parseInt(getRevision(SystemProperties.inDevMode)), context);
            findProperty("inDevMode[]").change(SystemProperties.inDevMode, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();
        }
    }
}