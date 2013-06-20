package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;

public class ClientBackupActionProperty extends ScriptingActionProperty {

    public ClientBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            new BackupActionProperty(LM).executeCustom(context);

            String lastBackupFilePath = (String) LM.findLCPByCompoundName("backupFilePath").read(context.getSession());
            String backupFileName = ((String) LM.findLCPByCompoundName("backupFileName").read(context.getSession())).trim();
            if (lastBackupFilePath != null) {
                lastBackupFilePath = lastBackupFilePath.trim();
                File file = new File(lastBackupFilePath);
                if (file.exists()) {
                    context.delayUserInterfaction(new ExportFileClientAction(backupFileName, IOUtils.getFileBytes(file)));
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
