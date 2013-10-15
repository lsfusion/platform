package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.util.Iterator;

public class SaveBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public SaveBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("Backup")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataObject backupObject = context.getDataKeyValue(backupInterface);

            String fileBackup = ((String) LM.findLCPByCompoundName("fileBackup").read(context.getSession(), backupObject));
            String fileBackupName = ((String) LM.findLCPByCompoundName("nameBackup").read(context.getSession(), backupObject));
            boolean fileDeletedBackup = LM.findLCPByCompoundName("fileDeletedBackup").read(context.getSession(), backupObject) != null;
            if (fileBackup != null && !fileDeletedBackup) {
                assert fileBackupName != null;
                File file = new File(fileBackup.trim());
                if (file.exists()) {
                    context.delayUserInterfaction(new ExportFileClientAction(fileBackupName.trim(), IOUtils.getFileBytes(file)));
                } else {
                    context.delayUserInterfaction(new MessageClientAction("Файл не найден", "Ошибка"));
                }
            } else {
                context.delayUserInterfaction(new MessageClientAction("Файл был удалён", "Ошибка"));
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
