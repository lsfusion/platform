package lsfusion.server.logics.service;

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
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.StatusMessage;
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

        Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, List<Object>> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, List<Object>> javaProcesses = getJavaProcesses(ThreadUtils.getAllThreads(), sqlJavaActiveThreads, readAllocatedBytes);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        // оставляем только javaProcesses + freeProcesses
        sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));

        if (!sqlProcesses.isEmpty())
            ServerLoggers.processDumpLogger.info(String.format("PROCESS DUMP: %s %s\n", sqlProcesses.size(), sqlProcesses.size() > 1 ? "processes" : "process"));
        for (String key : sqlProcesses.keys()) {
            List<Object> sqlProcess = sqlProcesses.getObject(key);
            List<Object> javaProcess = javaProcesses.getObject(key);

            String stackTraceJavaProcess = javaProcess == null ? null : (String) javaProcess.get(0);
            String nameJavaProcess = javaProcess == null ? null : (String) javaProcess.get(1);
            String statusJavaProcess = javaProcess == null ? null : (String) javaProcess.get(2);
            String lockNameJavaProcess = javaProcess == null ? null : (String) javaProcess.get(3);
            String nameComputerJavaProcess = javaProcess == null ? null : (String) javaProcess.get(4);
            String nameUserJavaProcess = javaProcess == null ? null : (String) javaProcess.get(5);
            String lsfStackTraceProcess = javaProcess == null ? null : (String) javaProcess.get(6);
            Long threadAllocatedBytesProcess = javaProcess == null ? null : (Long) javaProcess.get(7);
            Long lastThreadAllocatedBytesProcess = javaProcess == null ? null : (Long) javaProcess.get(8);
            if (javaProcess != null)
                javaProcesses = javaProcesses.remove(key);

            ServerLoggers.processDumpLogger.info(String.format("idThreadProcess: %s\n   dateTimeCallProcess: %s\n   addressUserSQLProcess: %s\n" +
                            "   dateTimeSQLProcess: %s\n   isActiveSQLProcess: %s\n   inTransactionSQLProcess: %s\n   startTransactionSQLProcess: %s\n" +
                            "   attemptCountSQLProcess: %s\n   statusMessageSQLProcess: %s\n   computerProcess: %s\n   userProcess: %s\n" +
                            "   lockOwnerIdProcess: %s\n   lockOwnerNameProcess: %s\n   idSQLProcess: %s\n   isDisabledNestLoopProcess: %s\n" +
                            "   queryTimeoutProcess: %s\n   nameJavaProcess: %s\n   statusJavaProcess: %s\n   lockNameJavaProcess: %s\n" +
                            "   nameComputerJavaProcess: %s\n   nameUserJavaProcess: %s\n   threadAllocatedBytesProcess: %s\n   lastThreadAllocatedBytesProcess: %s\n" +
                            "\nlsfStackTraceProcess: \n%s\n\nstackTraceJavaProcess: \n%s\nfullQuerySQLProcess: \n%s\n\n", key, sqlProcess.get(0), sqlProcess.get(5),
                    sqlProcess.get(6), sqlProcess.get(7), getInTransactionSQLProcess(sqlProcess), sqlProcess.get(10), sqlProcess.get(11), sqlProcess.get(12),
                    sqlProcess.get(4), sqlProcess.get(3), sqlProcess.get(13), sqlProcess.get(5), sqlProcess.get(15), sqlProcess.get(16), sqlProcess.get(17),
                    nameJavaProcess, statusJavaProcess, lockNameJavaProcess, nameComputerJavaProcess, nameUserJavaProcess, threadAllocatedBytesProcess,
                    lastThreadAllocatedBytesProcess, lsfStackTraceProcess, stackTraceJavaProcess, sqlProcess.get(2)));
        }

        for (String key : javaProcesses.keys()) {
            List<Object> javaProcess = javaProcesses.getObject(key);
            if (!key.equals(String.valueOf(Thread.currentThread().getId())))
                ServerLoggers.processDumpLogger.info(String.format("idThreadProcess: %s\n   nameJavaProcess: %s\n   statusJavaProcess: %s\n   lockNameJavaProcess: %s\n" +
                                "   nameComputerJavaProcess: %s\n   nameUserJavaProcess: %s\n   threadAllocatedBytesProcess: %s\n   lastThreadAllocatedBytesProcess: %s\n" +
                                "\nlsfStackTraceProcess: \n%s\n\nstackTraceJavaProcess: \n%s\n\n", key,
                        javaProcess.get(1), javaProcess.get(2), javaProcess.get(3), javaProcess.get(4), javaProcess.get(5), javaProcess.get(7),
                        javaProcess.get(8), javaProcess.get(6), javaProcess.get(0)));
        }

    }

    private Boolean getInTransactionSQLProcess(List<Object> sqlProcess) {
        Boolean fusionInTransaction = (Boolean) sqlProcess.get(8);
        Boolean baseInTransaction = (Boolean) sqlProcess.get(9);
        return baseInTransaction != null ? baseInTransaction : fusionInTransaction;
    }

    private ImMap<String, List<Object>> getMSSQLProcesses(ExecutionContext context, Map<Integer, List<Object>> sessionThreadMap,
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
        propertyReaders.exclAdd("session_id", IntegerClass.instance);
        propertyReaders.exclAdd("host_name", StringClass.get(100));
        propertyReaders.exclAdd("client_net_address", StringClass.get(100));
        propertyReaders.exclAdd("start_time", DateTimeClass.instance);
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap()
                , 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        MMap<String, List<Object>> mResultMap = MapFact.mMap(MapFact.<String, List<Object>>override());
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("text"));
            if (!query.equals(originalQuery)) {
                String fullQuery = null;
                boolean isDisabledNestLoop = false;
                Integer queryTimeout = null;
                Integer processId = (Integer) entry.get("session_id");
                List<Object> sessionThread = sessionThreadMap.get(processId);
                if (sessionThread != null) {
                    if (sessionThread.get(7) != null) {
                        fullQuery = (String) sessionThread.get(7);
                    }
                    isDisabledNestLoop = (boolean) sessionThread.get(8);
                    queryTimeout = (Integer) sessionThread.get(9);
                }
                String address = trimToNull((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");

                Thread javaThread = sessionThread == null ? null : (Thread) sessionThread.get(0);
                boolean baseInTransaction = sessionThread != null && (boolean) sessionThread.get(1);
                Long startTransaction = sessionThread == null ? null : (Long) sessionThread.get(2);
                String attemptCount = sessionThread == null ? "0" : (String) sessionThread.get(3);
                StatusMessage statusMessage = sessionThread == null ? null : (StatusMessage) sessionThread.get(4);

                String resultId = getMonitorId(javaThread, processId);

                if (!query.isEmpty()) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                mResultMap.add(resultId, Arrays.asList(query, fullQuery, null, null,
                        address, dateTime, null, null, baseInTransaction, startTransaction, attemptCount, statusMessage,
                        null, null, processId, isDisabledNestLoop, queryTimeout));
            }
        }
        return mResultMap.immutable();
    }

    private ImMap<String, List<Object>> getPostgresProcesses(ExecutionContext context, Map<Integer, List<Object>> sessionThreadMap,
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
        propertyReaders.exclAdd("pid", IntegerClass.instance);
        propertyReaders.exclAdd("usename", StringClass.get(100));
        propertyReaders.exclAdd("client_addr", PGObjectReader.instance);
        propertyReaders.exclAdd("query_start", DateTimeClass.instance);
        propertyReaders.exclAdd("state", StringClass.get(100));
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        MMap<String, List<Object>> mResultMap = MapFact.mMap(MapFact.<String, List<Object>>override());
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("query"));
            String state = trimToEmpty((String) entry.get("state"));

            boolean active = !query.isEmpty() && state.equals("active");
            if (!query.equals(originalQuery)) {
                Integer sqlId = (Integer) entry.get("pid");
                String address = trimToNull((String) entry.get("client_addr"));
                Timestamp dateTime = (Timestamp) entry.get("query_start");

                List<Object> sessionThread = sessionThreadMap.get(sqlId);
                Thread javaThread = sessionThread == null ? null : (Thread) sessionThread.get(0);
                boolean baseInTransaction = sessionThread != null && (boolean) sessionThread.get(1);
                Long startTransaction = sessionThread == null ? null : (Long) sessionThread.get(2);
                String attemptCount = sessionThread == null ? "0" : (String) sessionThread.get(3);
                StatusMessage statusMessage = sessionThread == null ? null : (StatusMessage) sessionThread.get(4);
                Integer userActiveTask = sessionThread == null ? null : (Integer) sessionThread.get(5);
                Integer computerActiveTask = sessionThread == null ? null : (Integer) sessionThread.get(6);
                String fullQuery = sessionThread == null || sessionThread.get(7) == null ? null : (String) sessionThread.get(7);
                boolean isDisabledNestLoop = sessionThread != null && (boolean) sessionThread.get(8);
                Integer queryTimeout = sessionThread == null ? null : (Integer) sessionThread.get(9);

                List<Object> lockingProcess = lockingMap.get(sqlId);
                Integer lockingSqlId = lockingProcess == null ? null : (Integer) lockingProcess.get(0);
                List<Object> lockingSessionThread = lockingSqlId == null ? null : sessionThreadMap.get(lockingSqlId);
                String lockOwnerId = lockingSessionThread == null ? null : getMonitorId((Thread) lockingSessionThread.get(0), lockingSqlId);
                String lockOwnerName = lockingProcess == null ? null : (String) lockingProcess.get(1);

                String resultId = getMonitorId(javaThread, sqlId);

                if (active) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                mResultMap.add(resultId, Arrays.asList(javaThread == null ? null : RemoteLoggerAspect.getDateTimeCall(javaThread.getId()),
                        query, fullQuery, userActiveTask, computerActiveTask, address, dateTime,
                        active, state.equals("idle in transaction"), baseInTransaction, startTransaction, attemptCount, statusMessage,
                        lockOwnerId, lockOwnerName, sqlId, isDisabledNestLoop, queryTimeout));
            }
        }
        return mResultMap.immutable();
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
        propertyReaders.exclAdd("blocked_pid", IntegerClass.instance);
        propertyReaders.exclAdd("blocked_user", StringClass.get(100));
        propertyReaders.exclAdd("blocking_pid", IntegerClass.instance);
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

    private ImMap<String, List<Object>> getJavaProcesses(ImSet<Thread> allThreads, ImSet<Thread> sqlThreads, boolean readAllocatedBytes) {
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

        MExclMap<String, List<Object>> mResultMap = MapFact.mExclMap();
        for (int i = 0, size = threads.size(); i < size; i++) {
            Thread thread = threads.get(i);
            List<Object> threadInfo = getThreadInfo(thread, allThreads != null && !sqlThreads.contains(thread), threadInfos[i], allocatedBytes == null ? null : allocatedBytes[i]);
            if (threadInfo != null) {
                long pid = thread.getId();
                mResultMap.exclAdd(String.valueOf(pid), threadInfo);
            }
        }
        return mResultMap.immutable();
    }

    private List<Object> getThreadInfo(Thread thread, boolean onlyActive, ThreadInfo threadInfo, Long allocatedBytes) {
        long id = thread.getId();

        String status = threadInfo == null ? null : String.valueOf(threadInfo.getThreadState());
        String stackTrace = threadInfo == null ? null : ThreadUtils.getJavaStack(threadInfo.getStackTrace());
        String name = threadInfo == null ? null : threadInfo.getThreadName();
        String lockName = threadInfo == null ? null : threadInfo.getLockName();
        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
        String computer = logInfo == null ? null : logInfo.hostnameComputer;
        String user = logInfo == null ? null : logInfo.userName;
        String lsfStack = getLSFStack(thread);
        Long lastAllocatedBytes = SQLSession.getThreadAllocatedBytes(allocatedBytes, id);

        return !onlyActive || ThreadUtils.isActiveJavaProcess(status, stackTrace, false) ? Arrays.asList((Object) stackTrace,
                name, status, lockName, computer, user, lsfStack, allocatedBytes, lastAllocatedBytes) : null;
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