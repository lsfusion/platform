package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                        File zipFile = null;
                        try {
                            zipFile = File.createTempFile("zip", ".zip");
                            try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
                                File[] files = file.listFiles();
                                if (files != null) {
                                    for (File f : files) {
                                        if (f.isFile()) {
                                            String fileName = f.getName();
                                            InputStream bis = new FileInputStream(f);
                                            zos.putNextEntry(new ZipEntry(fileName));
                                            byte[] buf = new byte[1024];
                                            int len;
                                            while ((len = bis.read(buf)) > 0) {
                                                zos.write(buf, 0, len);
                                            }
                                            bis.close();
                                        }
                                    }
                                }
                            }
                            context.delayUserInterfaction(new WriteClientAction(new RawFileData(zipFile), fileBackupName + ".zip", null, false, true));
                        } finally {
                            BaseUtils.safeDelete(zipFile);
                        }
                    } else {
                        context.delayUserInterfaction(new WriteClientAction(new RawFileData(file), fileBackupName, null, false, true));
                    }
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
