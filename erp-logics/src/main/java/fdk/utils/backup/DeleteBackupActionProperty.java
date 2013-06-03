package fdk.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.File;
import java.util.Iterator;

public class DeleteBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public DeleteBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, LM.findClassByCompoundName("Backup"));

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataSession session = context.createSession();
            DataObject backupObject = context.getDataKeyValue(backupInterface);

            String backupFilePath = (String) LM.findLCPByCompoundName("fileBackup").read(session, backupObject);
            String backupLogFilePath = (String) LM.findLCPByCompoundName("fileLogBackup").read(session, backupObject);
            File f = new File(backupFilePath);
            File fLog = new File(backupLogFilePath);
            if (fLog.exists()) {
                fLog.delete();
            }
            if (f.exists() && f.delete()) {
                LM.findLCPByCompoundName("fileDeletedBackup").change(true, session, backupObject);
            }
            session.apply(context.getBL());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
