package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.*;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.ZipUtils;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
                        Map<String, RawFileData> zippingFiles = new HashMap<>();
                        File[] files = file.listFiles();
                        if (files != null)
                            for (File f : files)
                                if (f.isFile())
                                    zippingFiles.put(f.getName(), new RawFileData(IOUtils.getFileBytes(f)));
                        FileData zipFile = ZipUtils.makeZipFile(zippingFiles, false);
                        writeFile(context, zipFile, fileBackupName);
                    } else {
                        writeFile(context, new FileData(new RawFileData(file), BaseUtils.getFileExtension(file)), BaseUtils.getFileName(fileBackupName));
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

    private void writeFile(ExecutionContext context, FileData file, String name) {
        context.delayUserInterfaction(new WriteClientAction(new NamedFileData(file, name), name, false, true));
    }
}
