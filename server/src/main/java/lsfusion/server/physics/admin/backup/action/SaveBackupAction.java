package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.remote.RemoteFiles;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.ZipUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

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
                    // a backup can be bigger than the max java array (2GB) and than the memory, so it is not read here : the client reads it in chunks
                    if (file.isDirectory()) {
                        File[] files = file.listFiles();
                        writeFile(context, ZipUtils.makeZipFile(files != null ? files : new File[0]), true, fileBackupName, "zip");
                    } else {
                        writeFile(context, file, false, BaseUtils.getFileName(fileBackupName), BaseUtils.getFileExtension(file));
                    }
                } else {
                    context.messageError(localize("{backup.file.not.found}"));
                }
            } else {
                context.messageError(localize("{backup.file.deleted}"));
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void writeFile(ExecutionContext context, File file, boolean temporary, String name, String extension) {
        context.delayUserInteraction(RemoteFiles.getWriteAction(file, temporary, name, extension, true));
    }
}
