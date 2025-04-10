package lsfusion.server.physics.dev.integration.external.to.file.report;

import lsfusion.base.file.FileData;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

public class CopyReportResources extends InternalAction {

    public CopyReportResources(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        FileData zipFile = (FileData) getParam(0, context);
        String md5 = (String) getParam(1, context);
        Object result = context.requestUserInteraction(new CopyReportResourcesClientAction(null, md5));
        if (result instanceof Boolean && !(boolean) result)
            result = context.requestUserInteraction(new CopyReportResourcesClientAction(zipFile, md5));
        if (result instanceof String)
            throw new RuntimeException("Copy report resources error: " + result);
    }
}
