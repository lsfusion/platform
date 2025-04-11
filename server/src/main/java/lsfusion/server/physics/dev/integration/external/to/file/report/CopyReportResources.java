package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class CopyReportResources extends InternalAction {

    public CopyReportResources(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String logicsName = (String) findProperty("logicsName[]").read(context);
            FileData zipFile = (FileData) getParam(0, context);
            String md5 = (String) getParam(1, context);
            Object result = context.requestUserInteraction(new CopyReportResourcesClientAction(logicsName, null, md5));
            if (result instanceof Boolean && !(boolean) result)
                result = context.requestUserInteraction(new CopyReportResourcesClientAction(logicsName, zipFile, md5));
            if (result instanceof String)
                throw new RuntimeException("Copy report resources error: " + result);
        } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
}
