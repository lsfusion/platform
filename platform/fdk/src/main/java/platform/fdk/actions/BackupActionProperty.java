package platform.fdk.actions;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupActionProperty extends ScriptingActionProperty {

    private String dumpDir;
    private DateFormat dateFormat;
    private Date date;
    private String binPath;

    public BackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            DataSession session = createSession();
            DataAdapter adapter = session.sql.adapter;
            dumpDir = (String) LM.findLCPByCompoundName("dumpDirBackupTask").read(session);
            if(dumpDir!=null)
                dumpDir = dumpDir.trim();
            binPath = (String) LM.findLCPByCompoundName("binPathBackupTask").read(session);
            if(binPath!=null)
                binPath = binPath.trim();

            Calendar calendar = Calendar.getInstance();

            date = calendar.getTime();
            dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

            String dateTime = dateFormat.format(date);
            File f = new File(dumpDir);
            f.mkdir();

            LM.getBL().backupDB(binPath, dumpDir + dateTime);
            Thread.sleep(500);
            DataObject backupObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("backup"));
            LM.findLCPByCompoundName("dateBackup").change(new java.sql.Date(date.getTime()), session, backupObject);
            LM.findLCPByCompoundName("timeBackup").change(new java.sql.Time(date.getTime()), session, backupObject);
            LM.findLCPByCompoundName("fileBackup").change(dumpDir + dateTime, session, backupObject);
            LM.findLCPByCompoundName("fileLogBackup").change(dumpDir + dateTime + "log.txt", session, backupObject);

            String log = "";
            Scanner scanner = new Scanner(new File(dumpDir + dateTime + "log.txt"), Charset.defaultCharset().name());
            while (scanner.hasNext()) {
                log +=scanner.nextLine() + "\r\n";
            }
            LM.findLCPByCompoundName("logBackup").change(log, session, backupObject);
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
