package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ThreadDumpClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class ThreadDumpActionProperty extends ScriptingAction {

    public ThreadDumpActionProperty(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        RawFileData threadDump = (RawFileData) context.requestUserInteraction(new ThreadDumpClientAction());
        if (threadDump != null) {
            try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                ObjectValue currentConnection = findProperty("currentConnection[]").readClasses(newContext);
                if(currentConnection instanceof DataObject) findProperty("fileThreadDump[Connection]").change(new FileData(threadDump, "txt"), newContext, (DataObject) currentConnection);
                newContext.apply();
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}