package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.Result;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.ResourceUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CopyReportResources extends InternalAction {

    public CopyReportResources(UtilsLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Map<String, RawFileData> files = new HashMap<>();
        readResource(files, "jasperreports.properties");
        readResource(files, "jasperreports_extension.properties");
        List<String> reportResources = ResourceUtils.getResources(Pattern.compile("/fonts/.*"));
        for(String reportResource : reportResources) {
            readResource(files, reportResource);
        }
        context.delayUserInteraction(new CopyReportResourcesClientAction(files));
    }

    private void readResource(Map<String, RawFileData> filesMap, String resource) {
        Result<String> path = new Result<>();
        RawFileData reportFile = ResourceUtils.findResourceAsFileData(resource, true, true, path, null);
        filesMap.put(path.result, reportFile);
    }
}
