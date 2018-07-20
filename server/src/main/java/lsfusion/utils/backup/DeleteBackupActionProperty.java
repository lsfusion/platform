package lsfusion.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;

public class DeleteBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public DeleteBackupActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try (DataSession session = context.createSession()) {
            DataObject backupObject = context.getDataKeyValue(backupInterface);

            String backupFilePath = (String) findProperty("file[Backup]").read(session, backupObject);
            String backupLogFilePath = (String) findProperty("fileLog[Backup]").read(session, backupObject);
            File f = new File(backupFilePath);
            File fLog = new File(backupLogFilePath);
            if (fLog.exists() && !fLog.delete()) {
                fLog.deleteOnExit();
            }
            if (!f.exists() || f.delete()) {
                findProperty("fileDeleted[Backup]").change(true, session, backupObject);
            }
            session.apply(context);

        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
