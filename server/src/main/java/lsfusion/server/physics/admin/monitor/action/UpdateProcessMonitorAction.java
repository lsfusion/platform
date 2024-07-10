package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.JavaProcess;
import lsfusion.server.physics.admin.monitor.sql.SQLProcess;
import lsfusion.server.physics.admin.monitor.sql.SQLThreadInfo;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.base.DateConverter.sqlTimestampToLocalDateTime;

public class UpdateProcessMonitorAction extends ProcessDumpAction {

    public UpdateProcessMonitorAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = Settings.get().isReadAllocatedBytes();
            String processType = trimToEmpty((String) findProperty("nameProcessType[]").read(context));
            context.cancel(SetFact.singleton((SessionDataProperty) findProperty("processType[]").property));

            updateProcessMonitor(context, processType, readAllocatedBytes);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void updateProcessMonitor(ExecutionContext context, String processType, boolean readAllocatedBytes) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        SQLSyntaxType syntaxType = context.getDbSyntax().getSyntaxType();

        boolean active = processType.endsWith("activeAll");
        boolean activeSQL = processType.isEmpty() || processType.endsWith("activeSQL");
        boolean activeJava = processType.endsWith("activeJava");

        boolean logSqlProcesses = Settings.get().isLogSqlProcesses();

        Map<Integer, SQLThreadInfo> sessionThreadMap = SQLSession.getSQLThreadMap();

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, SQLProcess> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL, logSqlProcesses)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, JavaProcess> javaProcesses = getJavaProcesses(activeSQL ? null : ThreadUtils.getAllThreads(),
                active || activeSQL ? sqlJavaActiveThreads : SetFact.EMPTY(), active || activeJava, readAllocatedBytes, logSqlProcesses);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        if (active) { // оставляем только javaProcesses + freeProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));
        }
        if (activeJava) { // оставляем javaProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys());
        }

        ImOrderSet<LP> propsJava = getProps(findProperties("idThreadProcess[STRING[10]]", "stackTraceJavaProcess[STRING[10]]", "nameJavaProcess[STRING[10]]",
                "statusJavaProcess[STRING[10]]", "lockNameJavaProcess[STRING[10]]", "lockOwnerIdProcess[STRING[10]]", "lockOwnerNameProcess[STRING[10]]",
                "nameComputerJavaProcess[STRING[10]]", "nameUserJavaProcess[STRING[10]]", "lsfStackTraceProcess[STRING[10]]",
                "threadAllocatedBytesProcess[STRING[10]]", "lastThreadAllocatedBytesProcess[STRING[10]]"));

        ImOrderSet<LP> propsSQL = getProps(findProperties("idThreadProcess[STRING[10]]", "dateTimeCallProcess[STRING[10]]",
                "querySQLProcess[STRING[10]]", "addressUserSQLProcess[STRING[10]]",
                "dateTimeSQLProcess[STRING[10]]", "isActiveSQLProcess[STRING[10]]", "inTransactionSQLProcess[STRING[10]]",
                "startTransactionSQLProcess[STRING[10]]", "attemptCountSQLProcess[STRING[10]]", "statusSQLProcess[STRING[10]]",
                "statusMessageSQLProcess[STRING[10]]", "waitEventTypeSQLProcess[STRING[10]]", "waitEventSQLProcess[STRING[10]]",
                "computerProcess[STRING[10]]", "userProcess[STRING[10]]", "lockOwnerIdProcess[STRING[10]]",
                "lockOwnerNameProcess[STRING[10]]", "fullQuerySQLProcess[STRING[10]]", "idSQLProcess[STRING[10]]",
                "isDisabledNestLoopProcess[STRING[10]]", "queryTimeoutProcess[STRING[10]]", "debugInfoSQLProcess[STRING[10]]",
                "threadNameSQLProcess[STRING[10]]", "threadStackTraceSQLProcess[STRING[10]]"));

        LP.change(context, propsJava, javaProcesses, StringClass.get(10), (key, value, lp) -> getJavaMapValue(lp, value, key));
        LP.change(context, propsSQL, sqlProcesses, StringClass.get(10), (key, value, lp) -> getSQLMapValue(lp, value, key));
    }

    private Object getJavaMapValue(LP<?> prop, JavaProcess javaProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread;
            case "stackTraceJavaProcess":
                return javaProcess.stackTrace;
            case "nameJavaProcess":
                return javaProcess.name;
            case "statusJavaProcess":
                return javaProcess.status;
            case "lockNameJavaProcess":
                return javaProcess.lockName;
            case "lockOwnerIdProcess":
                return javaProcess.lockOwnerId;
            case "lockOwnerNameProcess":
                return javaProcess.lockOwnerName;
            case "nameComputerJavaProcess":
                return javaProcess.computer;
            case "nameUserJavaProcess":
                return javaProcess.user;
            case "lsfStackTraceProcess":
                return javaProcess.lsfStackTrace;
            case "threadAllocatedBytesProcess":
                return javaProcess.threadAllocatedBytes;
            case "lastThreadAllocatedBytesProcess":
                return javaProcess.lastThreadAllocatedBytes;
            default:
                return null;
        }
    }

    private Object getSQLMapValue(LP<?> prop, SQLProcess sqlProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread;
            case "dateTimeCallProcess":
                return sqlProcess.dateTimeCall;
            case "querySQLProcess":
                return sqlProcess.query;
            case "fullQuerySQLProcess":
                return sqlProcess.fullQuery;
            case "userProcess":
                return sqlProcess.user;
            case "computerProcess":
                return sqlProcess.computer;
            case "addressUserSQLProcess":
                return sqlProcess.addressUser;
            case "dateTimeSQLProcess":
                return sqlProcess.dateTime;
            case "isActiveSQLProcess":
                return (sqlProcess.isActive != null && sqlProcess.isActive ? true : null);
            case "inTransactionSQLProcess":
                if (sqlProcess.baseInTransaction != null && sqlProcess.fusionInTransaction != null && sqlProcess.fusionInTransaction)
                    ServerLoggers.assertLog(sqlProcess.baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                return (sqlProcess.baseInTransaction != null ? sqlProcess.baseInTransaction : sqlProcess.fusionInTransaction) ? true : null;
            case "startTransactionSQLProcess":
                return sqlProcess.startTransaction != null ? sqlTimestampToLocalDateTime(new Timestamp(sqlProcess.startTransaction)) : null;
            case "attemptCountSQLProcess":
                return sqlProcess.attemptCount;
            case "statusSQLProcess":
                return sqlProcess.status;
            case "statusMessageSQLProcess":
                return sqlProcess.statusMessage != null ? sqlProcess.statusMessage.getMessage() : null;
            case "waitEventTypeSQLProcess":
                return sqlProcess.waitEventType;
            case "waitEventSQLProcess":
                return sqlProcess.waitEvent;
            case "lockOwnerIdProcess":
                return sqlProcess.lockOwnerId;
            case "lockOwnerNameProcess":
                return sqlProcess.lockOwnerName;
            case "idSQLProcess":
                return sqlProcess.sqlId;
            case "isDisabledNestLoopProcess":
                return sqlProcess.isDisabledNestLoop != null && sqlProcess.isDisabledNestLoop ? true : null;
            case "queryTimeoutProcess":
                return sqlProcess.queryTimeout;
            case "debugInfoSQLProcess":
                return sqlProcess.debugInfo;
            case "threadNameSQLProcess":
                return sqlProcess.threadName;
            case "threadStackTraceSQLProcess":
                return sqlProcess.threadStackTrace;
            default:
                return null;
        }
    }

    private ImOrderSet<LP> getProps(LP<?>[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<LP>(Arrays.asList(properties))).addOrderExcl(LM.baseLM.importedString);
    }
}