package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class CheckCopyReportResources extends InternalAction {

    public CheckCopyReportResources(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String logicsName = (String) findProperty("logicsName[]").read(context);
            String md5 = (String) getParam(0, context);
            Object result = context.requestUserInteraction(new CopyReportResourcesClientAction(logicsName, null, md5));
            if (result instanceof Boolean && !(boolean) result)
                findProperty("needCopyReportResources[]").change(new DataObject(true), context);
            else if (result instanceof String)
                throw new RuntimeException("Check copy report resources error: " + result);
        } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
}
