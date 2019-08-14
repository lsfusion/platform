package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;

public class SaveBackupAction extends InternalAction {
    private final ClassPropertyInterface backupInterface;

    public SaveBackupAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataObject backupObject = context.getDataKeyValue(backupInterface);

            String fileBackup = ((String) findProperty("file[Backup]").read(context.getSession(), backupObject));
            String fileBackupName = ((String) findProperty("name[Backup]").read(context.getSession(), backupObject));
            boolean fileDeletedBackup = findProperty("fileDeleted[Backup]").read(context.getSession(), backupObject) != null;
            if (fileBackup != null && !fileDeletedBackup) {
                assert fileBackupName != null;
                File file = new File(fileBackup.trim());
                if (file.exists()) {
                    context.delayUserInterfaction(new WriteClientAction(new RawFileData(file), fileBackupName, null, false, true));
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
