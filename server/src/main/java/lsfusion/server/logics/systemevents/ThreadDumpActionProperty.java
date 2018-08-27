package lsfusion.server.logics.systemevents;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.interop.action.ThreadDumpClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.SystemEventsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ThreadDumpActionProperty extends ScriptingActionProperty {

    public ThreadDumpActionProperty(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        byte[] threadDump = (byte[]) context.requestUserInteraction(new ThreadDumpClientAction());
        if (threadDump != null) {
            try (DataSession session = context.createSession()) {
                ObjectValue currentConnection = findProperty("currentConnection[]").readClasses(session);
                if(currentConnection instanceof DataObject)
                    findProperty("fileThreadDump[Connection]").change(BaseUtils.mergeFileAndExtension(threadDump, "txt".getBytes()), session, (DataObject) currentConnection);
                session.apply(context);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}