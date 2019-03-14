package lsfusion.server.physics.admin.backup;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;

public class SaveBackupActionProperty extends ScriptingAction {
    private final ClassPropertyInterface backupInterface;

    public SaveBackupActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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
