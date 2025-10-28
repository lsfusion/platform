package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.ZipUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
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
                    if (file.isDirectory()) {
                        File zipFile = ZipUtils.makeZipFile(file.listFiles());
                        try {
                            writeFile(context, zipFile, fileBackupName);
                        } finally {
                            BaseUtils.safeDelete(zipFile);
                        }
                    } else {
                        writeFile(context, file, BaseUtils.getFileName(fileBackupName));
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

    private void writeFile(ExecutionContext context, File file, String name) throws IOException {
        context.delayUserInterfaction(new WriteClientAction(new NamedFileData(new FileData(new RawFileData(file), BaseUtils.getFileExtension(file)), name), name, false, true));
    }
}
