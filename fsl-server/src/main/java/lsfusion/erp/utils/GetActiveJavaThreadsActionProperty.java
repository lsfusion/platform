package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.util.Map;

public class GetActiveJavaThreadsActionProperty extends ScriptingActionProperty {

    public GetActiveJavaThreadsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            getActiveJavaThreads(context);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    private void getActiveJavaThreads(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        DataSession session = context.getSession();
        ThreadMXBean thMxB = ManagementFactory.getThreadMXBean();

        ThreadInfo[] threadsInfo = thMxB.dumpAllThreads(true, false);

        ServerLoggers.systemLogger.info("GetActiveJavaThreads: started");

        session.dropChanges((DataProperty) findProperty("idActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("stackTraceActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("nameActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("statusActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("lockNameActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("lockOwnerIdActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("lockOwnerNameActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("computerActiveJavaThread[INTEGER]").property);
        session.dropChanges((DataProperty) findProperty("userActiveJavaThread[INTEGER]").property);

        Map<Long, Thread> threadMap = ThreadUtils.getThreadMap();
        for (ThreadInfo threadInfo : threadsInfo) {
            long id = threadInfo.getThreadId();
            Thread thread = threadMap.get(id);
            DataObject currentObject = new DataObject(id);

            String status = String.valueOf(threadInfo.getThreadState());
            String stackTrace = stackTraceToString(threadInfo.getStackTrace());
            String name = threadInfo.getThreadName();
            String lockName = threadInfo.getLockName();
            int lockOwnerId = (int) threadInfo.getLockOwnerId();
            String lockOwnerName = threadInfo.getLockOwnerName();
            LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
            String computer = logInfo == null ? null : logInfo.hostnameComputer;
            String user = logInfo == null ? null : logInfo.userName;

            ServerLoggers.systemLogger.info("GetActiveJavaThreads: thread " + name);

            findProperty("idActiveJavaThread[INTEGER]").change(id, session, currentObject);
            findProperty("stackTraceActiveJavaThread[INTEGER]").change(stackTrace, session, currentObject);
            findProperty("nameActiveJavaThread[INTEGER]").change(name, session, currentObject);
            findProperty("statusActiveJavaThread[INTEGER]").change(status, session, currentObject);
            findProperty("lockNameActiveJavaThread[INTEGER]").change(lockName, session, currentObject);
            findProperty("lockOwnerIdActiveJavaThread[INTEGER]").change(lockOwnerId, session, currentObject);
            findProperty("lockOwnerNameActiveJavaThread[INTEGER]").change(lockOwnerName, session, currentObject);
            findProperty("computerActiveJavaThread[INTEGER]").change(computer, session, currentObject);
            findProperty("userActiveJavaThread[INTEGER]").change(user, session, currentObject);
        }

        ServerLoggers.systemLogger.info("GetActiveJavaThreads: finished");
    }

    private String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}