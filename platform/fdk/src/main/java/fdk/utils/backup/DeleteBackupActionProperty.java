package fdk.utils.backup;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;

public class DeleteBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public DeleteBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("backup")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataSession session = context.createSession();
            DataObject backupObject = context.getKeyValue(backupInterface);

            String fileAddress = (String) LM.findLCPByCompoundName("fileBackup").read(session, backupObject);
            String fileLogAddress = (String) LM.findLCPByCompoundName("fileLogBackup").read(session, backupObject);
            File f = new File(fileAddress);
            File fLog = new File(fileLogAddress);
            if(fLog.exists())
                fLog.delete();
            if (f.exists() && f.delete())
                LM.findLCPByCompoundName("fileDeletedBackup").change(true, session, backupObject);
            session.apply(context.getBL());

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
