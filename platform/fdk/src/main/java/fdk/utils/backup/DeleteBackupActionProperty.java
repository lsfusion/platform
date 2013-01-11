package fdk.utils.backup;

import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Scanner;

public class DeleteBackupActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface backupInterface;

    public DeleteBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("backup")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backupInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataSession session = createSession();
            DataObject backupObject = context.getKeyValue(backupInterface);

            String fileAddress = (String) LM.findLCPByCompoundName("fileBackup").read(session, backupObject);
            String fileLogAddress = (String) LM.findLCPByCompoundName("fileLogBackup").read(session, backupObject);
            File f = new File(fileAddress);
            File fLog = new File(fileLogAddress);
            if(fLog.exists())
                fLog.delete();
            if (f.exists() && f.delete())
                LM.findLCPByCompoundName("fileDeletedBackup").change(true, session, backupObject);
            session.apply(LM.getBL());

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
