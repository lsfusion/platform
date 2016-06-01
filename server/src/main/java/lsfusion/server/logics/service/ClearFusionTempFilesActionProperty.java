package lsfusion.server.logics.service;

import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.io.File;
import java.sql.SQLException;
import java.util.Calendar;

public class ClearFusionTempFilesActionProperty extends ScriptingActionProperty {

    public ClearFusionTempFilesActionProperty(ServiceLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer countDays = (Integer) findProperty("countDaysClearFusionTempFiles").read(context);
            if (countDays != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -countDays);
                long minDate = cal.getTimeInMillis();
                String tempDir = System.getProperty("java.io.tmpdir");
                if (tempDir != null) {
                    deleteFiles(new File(tempDir), minDate, false);
                }
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
                                safeDelete(file);
                            }
                        } else if (force || needDeleteFile(file))
                            safeDelete(file);
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
                || name.startsWith("+~JF"); //jasper font files
    }

    private void safeDelete(File file) {
        if (!file.delete())
            file.deleteOnExit();
    }
}