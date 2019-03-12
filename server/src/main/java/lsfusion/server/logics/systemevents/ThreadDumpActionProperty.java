package lsfusion.server.logics.systemevents;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ThreadDumpClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class ThreadDumpActionProperty extends ScriptingActionProperty {

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