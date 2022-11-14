package lsfusion.server.physics.admin.service.action;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.localDateTimeToSqlTimestamp;

public class ClearFusionTempFilesAction extends InternalAction {

    public ClearFusionTempFilesAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            Integer countDays = (Integer) findProperty("countDaysClearFusionTempFiles").read(context);
            LocalDateTime minDate = LocalDateTime.now().minusDays(countDays != null ? countDays : 0);
            String tempDir = System.getProperty("java.io.tmpdir");
            if (tempDir != null) {
                deleteFiles(new File(tempDir), localDateTimeToSqlTimestamp(minDate).getTime(), false);
            }
        } catch (Exception e) {
            ServerLoggers.systemLogger.error("Failed to clear lsfusion temp files", e);
        }
    }

    private void deleteFiles(File dir, long minDate, boolean force) {
        if (dir != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.lastModified() < minDate) {
                        if (file.isDirectory()) {
                            if (force || needDeleteDir(file)) {
                                deleteFiles(file, minDate, true);
                                BaseUtils.safeDelete(file);
                            }
                        } else if (force || needDeleteFile(file))
                            BaseUtils.safeDelete(file);
                    }
                }
            }
        }
    }

    private boolean needDeleteDir(File dir) {
        String name = dir.getName();
        return name.startsWith("lsfusiondebug")
                || ((name.startsWith("gwtc") || name.startsWith("gwt-codeserver")) && name.endsWith(".tmp")); //GWT temp dirs
    }

    private boolean needDeleteFile(File file) {
        String name = file.getName();
        return name.startsWith("lsf")
                || name.startsWith("+~JF")//jasper font files
                || name.startsWith("ImageResourceGenerator"); //gwt temp files
    }
}