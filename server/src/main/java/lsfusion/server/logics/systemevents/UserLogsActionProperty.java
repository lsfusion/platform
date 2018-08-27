package lsfusion.server.logics.systemevents;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.UserLogsClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UserLogsActionProperty extends ScriptingActionProperty {

    public UserLogsActionProperty(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Map<String, byte[]> logFiles = (Map<String, byte[]>) context.requestUserInteraction(new UserLogsClientAction());
        if (logFiles != null && !logFiles.isEmpty()) {
            File zipFile = null;
            try {
                zipFile = makeZipFile(logFiles);
                try (DataSession session = context.createSession()) {
                    ObjectValue currentConnection = findProperty("currentConnection[]").readClasses(session);
                    if (currentConnection instanceof DataObject)
                        findProperty("fileUserLogs[Connection]").change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(zipFile), "zip".getBytes()), session, (DataObject) currentConnection);
                    session.apply(context);
                } catch (ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (zipFile != null && !zipFile.delete())
                    zipFile.deleteOnExit();
            }
        }
    }

    private File makeZipFile(Map<String, byte[]> logFiles) throws IOException {
        File zipFile = File.createTempFile("zip", ".zip");
        FileOutputStream fos = new FileOutputStream(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Map.Entry<String, byte[]> logFile : logFiles.entrySet()) {
                ByteArrayInputStream bis = new ByteArrayInputStream(logFile.getValue());
                zos.putNextEntry(new ZipEntry(logFile.getKey()));
                byte[] buf = new byte[1024];
                int len;
                while ((len = bis.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                bis.close();
            }
        }
        return zipFile;
    }
}