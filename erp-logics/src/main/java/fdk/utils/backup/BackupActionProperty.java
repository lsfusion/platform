package fdk.utils.backup;

import com.google.common.base.Throwables;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static platform.base.IOUtils.readFileToString;

public class BackupActionProperty extends ScriptingActionProperty {

    public BackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.createSession();

            Date currentDate = Calendar.getInstance().getTime();
            long currentTime = currentDate.getTime();
            String backupFileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(currentDate);

            String backupFilePath = context.getDbManager().backupDB(backupFileName);
            if (backupFilePath != null) {
                String backupFileLogPath = backupFilePath + ".log";

                DataObject backupObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("Backup"));
                LM.findLCPByCompoundName("dateBackup").change(new java.sql.Date(currentTime), session, backupObject);
                LM.findLCPByCompoundName("timeBackup").change(new java.sql.Time(currentTime), session, backupObject);
                LM.findLCPByCompoundName("fileBackup").change(backupFilePath, session, backupObject);
                LM.findLCPByCompoundName("fileLogBackup").change(backupFileLogPath, session, backupObject);

                LM.findLCPByCompoundName("logBackup").change(readFileToString(backupFileLogPath), session, backupObject);

                session.apply(context.getBL());

                LM.findLCPByCompoundName("fileNameBackup").change(backupFilePath, context.getSession());
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps((CalcProperty) LM.findLCPByCompoundName("dateBackup").property, (CalcProperty) LM.findLCPByCompoundName("timeBackup").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
