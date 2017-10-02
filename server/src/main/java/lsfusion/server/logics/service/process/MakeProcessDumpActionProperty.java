package lsfusion.server.logics.service.process;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.context.ThreadType;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.CustomReader;
import lsfusion.server.logics.PGObjectReader;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.remote.RemoteLoggerAspect;
import lsfusion.server.stack.ExecutionStackAspect;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.base.BaseUtils.trimToNull;

public class MakeProcessDumpActionProperty extends ScriptingActionProperty {

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

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

        Map<Integer, SQLThreadInfo> sessionThreadMap = SQLSession.getSQLThreadMap();

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, SQLProcess> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, JavaProcess> javaProcesses = getJavaProcesses(ThreadUtils.getAllThreads(), sqlJavaActiveThreads, readAllocatedBytes);

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

            ServerLoggers.processDumpLogger.info(String.format("idThreadProcess: %s\n   dateTimeCallProcess: %s\n   threadTypeProcess: %s\n   addressUserSQLProcess: %s\n" +
                            "   dateTimeSQLProcess: %s\n   isActiveSQLProcess: %s\n   inTransactionSQLProcess: %s\n   startTransactionSQLProcess: %s\n" +
                            "   attemptCountSQLProcess: %s\n   statusSQLProcess: %s\n   statusMessageSQLProcess: %s\n   computerProcess: %s\n   userProcess: %s\n" +
                            "   lockOwnerIdProcess: %s\n   lockOwnerNameProcess: %s\n   idSQLProcess: %s\n   isDisabledNestLoopProcess: %s\n" +
                            "   queryTimeout: %s\n   nameJavaProcess: %s\n   statusJavaProcess: %s\n   lockNameJavaProcess: %s\n" +
                            "   nameComputerJavaProcess: %s\n   nameUserJavaProcess: %s\n   threadAllocatedBytesProcess: %s\n   lastThreadAllocatedBytesProcess: %s\n" +
                            "\nlsfStackTraceProcess: \n%s\n\nstackTraceJavaProcess: \n%s\nfullQuerySQLProcess: \n%s\n\n", key, sqlProcess.dateTimeCall, sqlProcess.threadType,
                    sqlProcess.addressUser, sqlProcess.dateTime, sqlProcess.isActive, getInTransactionSQLProcess(sqlProcess), sqlProcess.startTransaction, sqlProcess.attemptCount,
                    sqlProcess.status, sqlProcess.statusMessage, sqlProcess.computer, sqlProcess.user, sqlProcess.lockOwnerId, sqlProcess.lockOwnerName, sqlProcess.sqlId, sqlProcess.isDisabledNestLoop,
                    sqlProcess.queryTimeout, nameJavaProcess, statusJavaProcess, lockNameJavaProcess, nameComputerJavaProcess, nameUserJavaProcess, threadAllocatedBytesProcess,
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

    private DataClass processIDType = IntegerClass.instance;

    private ImMap<String, SQLProcess> getMSSQLProcesses(ExecutionContext context, Map<Integer, SQLThreadInfo> sessionThreadMap,
                                                        MSet<Thread> javaThreads, MExclSet<String> mFreeSQLProcesses)
            throws SQLException, SQLHandledException {
        String originalQuery = "Select A.session_id,B.start_time, A.[host_name], A.[login_name], C.client_net_address, text\n" +
                "from sys.dm_exec_sessions A\n" +
                "Left Join sys.dm_exec_requests B\n" +
                "On A.[session_id]=B.[session_id]\n" +
                "Left Join sys.dm_exec_connections C\n" +
                "On A.[session_id]=C.[session_id]\n" +
                "CROSS APPLY sys.dm_exec_sql_text(sql_handle) AS sqltext";

        MExclSet<String> keyNames = SetFact.mExclSet();
        keyNames.exclAdd("numberrow");
        keyNames.immutable();

        MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
        keyReaders.exclAdd("numberrow", new CustomReader());
        keyReaders.immutable();

        MExclSet<String> propertyNames = SetFact.mExclSet();
        propertyNames.exclAdd("text");
        propertyNames.exclAdd("session_id");
        propertyNames.exclAdd("host_name");
        propertyNames.exclAdd("client_net_address");
        propertyNames.exclAdd("start_time");
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("text", StringClass.get(1000));
        propertyReaders.exclAdd("session_id", processIDType);
        propertyReaders.exclAdd("host_name", StringClass.get(100));
        propertyReaders.exclAdd("client_net_address", StringClass.get(100));
        propertyReaders.exclAdd("start_time", DateTimeClass.instance);
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap()
                , 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<String, SQLProcess> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("text"));
            if (!query.equals(originalQuery)) {
                String fullQuery = null;
                boolean isDisabledNestLoop = false;
                Integer queryTimeout = null;
                Integer processId = (Integer) entry.get("session_id");
                SQLThreadInfo sessionThread = sessionThreadMap.get(processId);
                if (sessionThread != null) {
                    fullQuery = sessionThread.fullQuery;
                    isDisabledNestLoop = sessionThread.isDisabledNestLoop;
                    queryTimeout = sessionThread.queryTimeout;
                }
                String address = trimToNull((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");

                Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;
                boolean baseInTransaction = sessionThread != null && sessionThread.baseInTransaction;
                Long startTransaction = sessionThread == null ? null : sessionThread.startTransaction;
                String attemptCount = sessionThread == null ? "0" : sessionThread.attemptCount;
                StatusMessage statusMessage = sessionThread == null ? null : sessionThread.statusMessage;

                String resultId = getMonitorId(javaThread, processId);

                if (!query.isEmpty()) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                Timestamp dateTimeCall = javaThread == null ? null : RemoteLoggerAspect.getDateTimeCall(javaThread.getId());
                ThreadType threadType = javaThread == null ? null : RemoteLoggerAspect.getThreadType(javaThread.getId());

                SQLProcess newEntry = new SQLProcess(dateTimeCall, threadType, query, fullQuery, null, null,
                        address, dateTime, null, null, baseInTransaction, startTransaction, attemptCount, null,
                        statusMessage, null, null, processId, isDisabledNestLoop, queryTimeout, null);
                SQLProcess prevEntry = resultMap.put(resultId, newEntry);

                if (prevEntry != null) {
                    Timestamp prevDateTime = prevEntry.dateTime;
                    if (prevDateTime != null && (dateTime == null || prevDateTime.getTime() > dateTime.getTime())) {
                        resultMap.put("s" + prevEntry.sqlId, prevEntry);
                    } else {
                        resultMap.put(resultId, prevEntry);
                        resultMap.put("s" + processId, newEntry);
                    }
                }
            }
        }
        return MapFact.fromJavaMap(resultMap);
    }

    private ImMap<String, SQLProcess> getPostgresProcesses(ExecutionContext context, Map<Integer, SQLThreadInfo> sessionThreadMap,
                                                           MSet<Thread> javaThreads, MExclSet<String> mFreeSQLProcesses)
            throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Map<Integer, List<Object>> lockingMap = getPostgresLockMap(context);

        String originalQuery = String.format("SELECT * FROM pg_stat_activity WHERE datname='%s'", context.getBL().getDataBaseName());

        MExclSet<String> keyNames = SetFact.mExclSet();
        keyNames.exclAdd("numberrow");
        keyNames.immutable();

        MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
        keyReaders.exclAdd("numberrow", new CustomReader());
        keyReaders.immutable();

        MExclSet<String> propertyNames = SetFact.mExclSet();
        propertyNames.exclAdd("query");
        propertyNames.exclAdd("pid");
        propertyNames.exclAdd("usename");
        propertyNames.exclAdd("client_addr");
        propertyNames.exclAdd("query_start");
        propertyNames.exclAdd("state");
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("query", StringClass.get(1000));
        propertyReaders.exclAdd("pid", processIDType);
        propertyReaders.exclAdd("usename", StringClass.get(100));
        propertyReaders.exclAdd("client_addr", PGObjectReader.instance);
        propertyReaders.exclAdd("query_start", DateTimeClass.instance);
        propertyReaders.exclAdd("state", StringClass.get(100));
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<String, SQLProcess> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("query"));
            String state = trimToEmpty((String) entry.get("state"));

            boolean active = !query.isEmpty() && state.equals("active");
            if (!query.equals(originalQuery)) {
                Integer sqlId = (Integer) entry.get("pid");
                String address = trimToNull((String) entry.get("client_addr"));
                Timestamp dateTime = (Timestamp) entry.get("query_start");

                SQLThreadInfo sessionThread = sessionThreadMap.get(sqlId);
                Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;
                boolean baseInTransaction = sessionThread != null && sessionThread.baseInTransaction;
                Long startTransaction = sessionThread == null ? null : sessionThread.startTransaction;
                String attemptCount = sessionThread == null ? "0" : sessionThread.attemptCount;
                Long userActiveTask = sessionThread == null ? null : sessionThread.userActiveTask;
                Long computerActiveTask = sessionThread == null ? null : sessionThread.computerActiveTask;
                String fullQuery = sessionThread == null ? null : sessionThread.fullQuery;
                boolean isDisabledNestLoop = sessionThread != null && sessionThread.isDisabledNestLoop;
                Integer queryTimeout = sessionThread == null ? null : sessionThread.queryTimeout;
                StatusMessage statusMessage = sessionThread == null ? null : sessionThread.statusMessage;

                List<Object> lockingProcess = lockingMap.get(sqlId);
                Integer lockingSqlId = lockingProcess == null ? null : (Integer) lockingProcess.get(0);
                SQLThreadInfo lockingSessionThread = lockingSqlId == null ? null : sessionThreadMap.get(lockingSqlId);
                String lockOwnerId = lockingSessionThread == null ? null : getMonitorId(lockingSessionThread.javaThread, lockingSqlId);
                String lockOwnerName = lockingProcess == null ? null : (String) lockingProcess.get(1);

                String resultId = getMonitorId(javaThread, sqlId);

                if (active) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                Timestamp dateTimeCall = javaThread == null ? null : RemoteLoggerAspect.getDateTimeCall(javaThread.getId());
                ThreadType threadType = javaThread == null ? null : RemoteLoggerAspect.getThreadType(javaThread.getId());

                SQLProcess newEntry = new SQLProcess(dateTimeCall, threadType, query, fullQuery, userActiveTask, computerActiveTask,
                        address, dateTime, active, state.equals("idle in transaction"), baseInTransaction, startTransaction, attemptCount,
                        state, statusMessage, lockOwnerId, lockOwnerName, sqlId, isDisabledNestLoop, queryTimeout, null);
                SQLProcess prevEntry = resultMap.put(resultId, newEntry);

                if (prevEntry != null) {
                    Timestamp prevDateTime = prevEntry.dateTime;
                    if (prevDateTime != null && (dateTime == null || prevDateTime.getTime() > dateTime.getTime())) {
                        resultMap.put("s" + prevEntry.sqlId, prevEntry);
                    } else {
                        resultMap.put(resultId, prevEntry);
                        resultMap.put("s" + sqlId, newEntry);
                    }
                }
            }
        }
        return MapFact.fromJavaMap(resultMap);
    }

    private Map<Integer, List<Object>> getPostgresLockMap(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        SQLSession sql = context.getSession().sql;

        String originalQuery = "SELECT blocked_locks.pid     AS blocked_pid,\n" +
                "         blocked_activity.usename  AS blocked_user,\n" +
                "         blocking_locks.pid     AS blocking_pid,\n" +
                "         blocking_activity.usename AS blocking_user,\n" +
                "         blocked_activity.query    AS blocked_statement,\n" +
                "         blocking_activity.query   AS blocking_statement\n" +
                "   FROM  pg_catalog.pg_locks         blocked_locks\n" +
                "    JOIN pg_catalog.pg_stat_activity blocked_activity  ON blocked_activity.pid = blocked_locks.pid\n" +
                "    JOIN pg_catalog.pg_locks         blocking_locks \n" +
                "        ON blocking_locks.locktype = blocked_locks.locktype\n" +
                "        AND blocking_locks.DATABASE IS NOT DISTINCT FROM blocked_locks.DATABASE\n" +
                "        AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation\n" +
                "        AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page\n" +
                "        AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple\n" +
                "        AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid\n" +
                "        AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid\n" +
                "        AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid\n" +
                "        AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid\n" +
                "        AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid\n" +
                "        AND blocking_locks.pid != blocked_locks.pid\n" +
                "    JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid\n" +
                "   WHERE NOT blocked_locks.granted";

        MExclSet<String> keyNames = SetFact.mExclSet();
        keyNames.exclAdd("numberrow");
        keyNames.immutable();

        MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
        keyReaders.exclAdd("numberrow", new CustomReader());
        keyReaders.immutable();

        MExclSet<String> propertyNames = SetFact.mExclSet();
        propertyNames.exclAdd("blocked_pid");
        propertyNames.exclAdd("blocked_user");
        propertyNames.exclAdd("blocking_pid");
        propertyNames.exclAdd("blocking_user");
        propertyNames.exclAdd("blocked_statement");
        propertyNames.exclAdd("blocking_statement");
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("blocked_pid", processIDType);
        propertyReaders.exclAdd("blocked_user", StringClass.get(100));
        propertyReaders.exclAdd("blocking_pid", processIDType);
        propertyReaders.exclAdd("blocking_user", StringClass.get(100));
        propertyReaders.exclAdd("blocked_statement", StringClass.get(100));
        propertyReaders.exclAdd("blocking_statement", StringClass.get(100));
        propertyReaders.immutable();

        ImOrderMap rs = sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders, true);

        Map<Integer, List<Object>> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {
            HMap entry = (HMap) rsValue;
            Integer blocked_pid = (Integer) entry.get("blocked_pid");
            Integer blocking_pid = (Integer) entry.get("blocking_pid");
            String blocking_statement = trim((String) entry.get("blocking_statement"), 100);
            resultMap.put(blocked_pid, Arrays.<Object>asList(blocking_pid, blocking_statement));
        }
        return resultMap;
    }

    private String getMonitorId(Thread javaThread, Integer processId) {
        return javaThread != null ? String.valueOf(javaThread.getId()) : ("s" + processId);
    }

    private ImMap<String, JavaProcess> getJavaProcesses(ImSet<Thread> allThreads, ImSet<Thread> sqlThreads, boolean readAllocatedBytes) {
        ImSet<Thread> threads;
        if (allThreads != null) {
            threads = allThreads;
        } else {
            threads = sqlThreads;
        }

        long[] threadIds = new long[threads.size()];
        for (int i = 0, size = threads.size(); i < size; i++) {
            threadIds[i] = threads.get(i).getId();
        }
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos;
        if (threadIds.length > 0) // https://bugs.openjdk.java.net/browse/JDK-8074815
            threadInfos = tBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
        else
            threadInfos = new ThreadInfo[]{};

        long[] allocatedBytes = null;
        if (readAllocatedBytes && tBean instanceof com.sun.management.ThreadMXBean) {
            allocatedBytes = ((com.sun.management.ThreadMXBean) tBean).getThreadAllocatedBytes(threadIds);
        }

        MExclMap<String, JavaProcess> mResultMap = MapFact.mExclMap();
        for (int i = 0, size = threads.size(); i < size; i++) {
            Thread thread = threads.get(i);
            JavaProcess threadInfo = getJavaProcess(thread, allThreads != null && !sqlThreads.contains(thread), threadInfos[i], allocatedBytes == null ? null : allocatedBytes[i]);
            if (threadInfo != null) {
                long pid = thread.getId();
                mResultMap.exclAdd(String.valueOf(pid), threadInfo);
            }
        }
        return mResultMap.immutable();
    }

    private JavaProcess getJavaProcess(Thread thread, boolean onlyActive, ThreadInfo threadInfo, Long allocatedBytes) {
        String status = threadInfo == null ? null : String.valueOf(threadInfo.getThreadState());
        String stackTrace = threadInfo == null ? null : ThreadUtils.getJavaStack(threadInfo.getStackTrace());
        String name = threadInfo == null ? null : threadInfo.getThreadName();
        String lockName = threadInfo == null ? null : threadInfo.getLockName();
        String lockOwnerId = threadInfo == null ? null : String.valueOf(threadInfo.getLockOwnerId());
        String lockOwnerName = threadInfo == null ? null : threadInfo.getLockOwnerName();
        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
        String computer = logInfo == null ? null : logInfo.hostnameComputer;
        String user = logInfo == null ? null : logInfo.userName;
        String lsfStack = getLSFStack(thread);
        Long lastAllocatedBytes = thread == null ? null : SQLSession.getThreadAllocatedBytes(allocatedBytes, thread.getId());

        return !onlyActive || ThreadUtils.isActiveJavaProcess(status, stackTrace, false) ? new JavaProcess(stackTrace, name, status, lockName, lockOwnerId,
                lockOwnerName, computer, user, lsfStack, allocatedBytes, lastAllocatedBytes) : null;
    }

    private String getLSFStack(Thread thread) {
        try {
            return thread == null ? null : ExecutionStackAspect.getStackString(thread, true, true);
        } catch (Exception e) {
            return null;
        }
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}