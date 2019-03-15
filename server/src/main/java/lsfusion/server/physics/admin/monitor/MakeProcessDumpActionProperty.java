package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.base.thread.ThreadUtils;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SQLThreadInfo;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;

import java.sql.SQLException;
import java.util.Map;

public class MakeProcessDumpActionProperty extends ProcessDumpActionProperty {

    public MakeProcessDumpActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = Settings.get().isReadAllocatedBytes();

            makeProcessDump(context, readAllocatedBytes);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void makeProcessDump(ExecutionContext context, boolean readAllocatedBytes) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        SQLSyntaxType syntaxType = context.getDbSyntax().getSyntaxType();

        Map<Integer, SQLThreadInfo> sessionThreadMap = SQLSession.getSQLThreadMap();

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, SQLProcess> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, true, false)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, true);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, JavaProcess> javaProcesses = getJavaProcesses(ThreadUtils.getAllThreads(), sqlJavaActiveThreads, true, readAllocatedBytes, false);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        // оставляем только javaProcesses + freeProcesses
        sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));

        if (!sqlProcesses.isEmpty())
            ServerLoggers.processDumpLogger.info(String.format("PROCESS DUMP: %s SQL %s\n", sqlProcesses.size(), sqlProcesses.size() > 1 ? "processes" : "process"));
        for (String key : sqlProcesses.keys()) {
            SQLProcess sqlProcess = sqlProcesses.getObject(key);
            JavaProcess javaProcess = javaProcesses.getObject(key);

            String stackTraceJavaProcess = javaProcess == null ? null : javaProcess.stackTrace;
            String nameJavaProcess = javaProcess == null ? null : javaProcess.name;
            String statusJavaProcess = javaProcess == null ? null : javaProcess.status;
            String lockNameJavaProcess = javaProcess == null ? null : javaProcess.lockName;
            String nameComputerJavaProcess = javaProcess == null ? null : javaProcess.computer;
            String nameUserJavaProcess = javaProcess == null ? null : javaProcess.user;
            String lsfStackTraceProcess = javaProcess == null ? null : javaProcess.lsfStackTrace;
            Long threadAllocatedBytesProcess = javaProcess == null ? null : javaProcess.threadAllocatedBytes;
            Long lastThreadAllocatedBytesProcess = javaProcess == null ? null : javaProcess.lastThreadAllocatedBytes;
            if (javaProcess != null)
                javaProcesses = javaProcesses.remove(key);

            ServerLoggers.processDumpLogger.info(String.format("idThreadProcess: %s\n   dateTimeCallProcess: %s\n   addressUserSQLProcess: %s\n" +
                            "   dateTimeSQLProcess: %s\n   isActiveSQLProcess: %s\n   inTransactionSQLProcess: %s\n   startTransactionSQLProcess: %s\n" +
                            "   attemptCountSQLProcess: %s\n   statusSQLProcess: %s\n   statusMessageSQLProcess: %s\n   computerProcess: %s\n   userProcess: %s\n" +
                            "   lockOwnerIdProcess: %s\n   lockOwnerNameProcess: %s\n   idSQLProcess: %s\n   isDisabledNestLoopProcess: %s\n" +
                            "   queryTimeout: %s\n   nameSQLProcess: %s\n   nameJavaProcess: %s\n   statusJavaProcess: %s\n   lockNameJavaProcess: %s\n" +
                            "   nameComputerJavaProcess: %s\n   nameUserJavaProcess: %s\n   threadAllocatedBytesProcess: %s\n   lastThreadAllocatedBytesProcess: %s\n" +
                            "\nlsfStackTraceProcess: \n%s\n\nstackTraceJavaProcess: \n%s\nfullQuerySQLProcess: \n%s\n\n", key, sqlProcess.dateTimeCall,
                    sqlProcess.addressUser, sqlProcess.dateTime, sqlProcess.isActive, getInTransactionSQLProcess(sqlProcess), sqlProcess.startTransaction, sqlProcess.attemptCount,
                    sqlProcess.status, sqlProcess.statusMessage, sqlProcess.computer, sqlProcess.user, sqlProcess.lockOwnerId, sqlProcess.lockOwnerName, sqlProcess.sqlId, sqlProcess.isDisabledNestLoop,
                    sqlProcess.queryTimeout, sqlProcess.threadName, nameJavaProcess, statusJavaProcess, lockNameJavaProcess, nameComputerJavaProcess, nameUserJavaProcess, threadAllocatedBytesProcess,
                    lastThreadAllocatedBytesProcess, lsfStackTraceProcess, stackTraceJavaProcess, sqlProcess.fullQuery));
        }

        int javaProcessesCount = javaProcesses.size() - 1; //свой процесс есть всегда
        if (javaProcessesCount > 0)
            ServerLoggers.processDumpLogger.info(String.format("PROCESS DUMP: %s JAVA %s\n", javaProcessesCount, javaProcessesCount > 1 ? "processes" : "process"));
        for (String key : javaProcesses.keys()) {
            JavaProcess javaProcess = javaProcesses.getObject(key);
            if (!key.equals(String.valueOf(Thread.currentThread().getId())))
                ServerLoggers.processDumpLogger.info(String.format("idThreadProcess: %s\n   nameJavaProcess: %s\n   statusJavaProcess: %s\n   lockNameJavaProcess: %s\n" +
                                "   nameComputerJavaProcess: %s\n   nameUserJavaProcess: %s\n   threadAllocatedBytesProcess: %s\n   lastThreadAllocatedBytesProcess: %s\n" +
                                "\nlsfStackTraceProcess: \n%s\n\nstackTraceJavaProcess: \n%s\n\n", key,
                        javaProcess.name, javaProcess.status, javaProcess.lockName, javaProcess.computer, javaProcess.user, javaProcess.threadAllocatedBytes,
                        javaProcess.lastThreadAllocatedBytes, javaProcess.lsfStackTrace, javaProcess.stackTrace));
        }

    }

    private Boolean getInTransactionSQLProcess(SQLProcess sqlProcess) {
        Boolean fusionInTransaction = sqlProcess.fusionInTransaction;
        Boolean baseInTransaction = sqlProcess.baseInTransaction;
        return baseInTransaction != null ? baseInTransaction : fusionInTransaction;
    }
}