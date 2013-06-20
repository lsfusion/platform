package lsfusion.erp.utils.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static lsfusion.base.IOUtils.readFileToString;

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
                String backupFileExtension = backupFilePath.substring(backupFilePath.lastIndexOf("."), backupFilePath.length());

                DataObject backupObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("Backup"));
                LM.findLCPByCompoundName("dateBackup").change(new java.sql.Date(currentTime), session, backupObject);
                LM.findLCPByCompoundName("timeBackup").change(new java.sql.Time(currentTime), session, backupObject);
                LM.findLCPByCompoundName("fileBackup").change(backupFilePath, session, backupObject);
                LM.findLCPByCompoundName("nameBackup").change(backupFileName + backupFileExtension, session, backupObject);
                LM.findLCPByCompoundName("fileLogBackup").change(backupFileLogPath, session, backupObject);

                LM.findLCPByCompoundName("logBackup").change(readFileToString(backupFileLogPath), session, backupObject);

                session.apply(context.getBL());

                LM.findLCPByCompoundName("backupFilePath").change(backupFilePath, context.getSession());
                LM.findLCPByCompoundName("backupFileName").change(backupFileName + backupFileExtension, context.getSession());
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
