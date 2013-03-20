package fdk.utils.backup;

import com.google.common.base.Throwables;
import platform.base.IOUtils;
import platform.interop.action.ExportFileClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientBackupActionProperty extends ScriptingActionProperty {

    public ClientBackupActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {

            new BackupActionProperty(LM).executeCustom(context);

            String fileName = (String) LM.findLCPByCompoundName("fileNameBackup").read(context.getSession());
            if (fileName != null) {
                fileName = fileName.trim();
                File file = new File(fileName);
                if (file.exists())
                    context.delayUserInterfaction(new ExportFileClientAction(fileName + ".backup", IOUtils.getFileBytes(file)));
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
