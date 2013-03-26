package fdk.utils.backup;

import com.google.common.base.Throwables;
import platform.base.IOUtils;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;

public class ClientBackupActionProperty extends ScriptingActionProperty {

    public ClientBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            new BackupActionProperty(LM).executeCustom(context);

            String lastBackupFilePath = (String) LM.findLCPByCompoundName("fileNameBackup").read(context.getSession());
            if (lastBackupFilePath != null) {
                lastBackupFilePath = lastBackupFilePath.trim();
                File file = new File(lastBackupFilePath);
                if (file.exists()) {
                    context.delayUserInterfaction(new ExportFileClientAction(lastBackupFilePath, IOUtils.getFileBytes(file)));
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
