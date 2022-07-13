package lsfusion.server.physics.admin.monitor.action;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.query.exec.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.type.reader.CustomReader;
import lsfusion.server.data.type.reader.PGObjectReader;
import lsfusion.server.data.type.reader.Reader;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.RemoteLoggerAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.JavaProcess;
import lsfusion.server.physics.admin.monitor.StatusMessage;
import lsfusion.server.physics.admin.monitor.sql.SQLProcess;
import lsfusion.server.physics.admin.monitor.sql.SQLThreadInfo;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.base.BaseUtils.trimToNull;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.sqlTimestampToLocalDateTime;

public abstract class ProcessDumpAction extends InternalAction {

    public ProcessDumpAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    protected ImMap<String, JavaProcess> getJavaProcesses(ImSet<Thread> allThreads, ImSet<Thread> sqlThreads, boolean onlyActive, boolean readAllocatedBytes, boolean logSqlProcesses) {
        ImSet<Thread> threads;
        if (allThreads != null) {
            threads = allThreads;
            if (logSqlProcesses) {
                ServerLoggers.exInfoLogger.info("getAllThreads");
                for (Thread entry : allThreads) {
                    ServerLoggers.exInfoLogger.info(String.format("ID: %s", entry.getId()));
                }
            }
        } else {
            threads = sqlThreads;
        }

        long[] threadIds = new long[threads.size()];
        for (int i = 0, size = threads.size(); i < size; i++) {
            threadIds[i] = threads.get(i).getId();
        }
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        java.lang.management.ThreadInfo[] threadInfos;
        if (threadIds.length > 0) // https://bugs.openjdk.java.net/browse/JDK-8074815
            threadInfos = tBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
        else
            threadInfos = new java.lang.management.ThreadInfo[]{};

        long[] allocatedBytes = null;
        Class threadMXBeanClass = ReflectionUtils.classForName("com.sun.management.ThreadMXBean");
        if (readAllocatedBytes && threadMXBeanClass != null && threadMXBeanClass.isInstance(tBean)) {
            allocatedBytes = ReflectionUtils.getMethodValue(threadMXBeanClass, tBean, "getThreadAllocatedBytes", new Class[]{long[].class}, new Object[] {threadIds});
        }

        MExclMap<String, JavaProcess> mResultMap = MapFact.mExclMap();
        for (int i = 0, size = threads.size(); i < size; i++) {
            Thread thread = threads.get(i);
            JavaProcess threadInfo = getJavaProcess(thread, onlyActive && allThreads != null && !sqlThreads.contains(thread), threadInfos[i], allocatedBytes == null ? null : allocatedBytes[i]);
            if (threadInfo != null) {
                long pid = thread.getId();
                mResultMap.exclAdd(String.valueOf(pid), threadInfo);
            }
        }
        return mResultMap.immutable();
    }

    private JavaProcess getJavaProcess(Thread thread, boolean onlyActive, java.lang.management.ThreadInfo threadInfo, Long allocatedBytes) {
        String status = threadInfo == null ? null : String.valueOf(threadInfo.getThreadState());
        String stackTrace = threadInfo == null ? null : ThreadUtils.getJavaStack(threadInfo.getStackTrace());
        String name = threadInfo == null ? null : threadInfo.getThreadName();
        String lockName = threadInfo == null ? null : threadInfo.getLockName();
        String lockOwnerId = threadInfo == null ? null : String.valueOf(threadInfo.getLockOwnerId());
        String lockOwnerName = threadInfo == null ? null : threadInfo.getLockOwnerName();
        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
        String computer = logInfo == null ? null : logInfo.hostnameComputer;
        String user = logInfo == null ? null : logInfo.userName;
        String lsfStack = ExecutionStackAspect.getLSFStack(thread);
        Long lastAllocatedBytes = thread == null ? null : SQLSession.getThreadAllocatedBytes(allocatedBytes, thread.getId());

        return !onlyActive || ThreadUtils.isActiveJavaProcess(status, stackTrace, false) ? new JavaProcess(stackTrace, name, status, lockName, lockOwnerId,
                lockOwnerName, computer, user, lsfStack, allocatedBytes, lastAllocatedBytes) : null;
    }

    private SQLProcess getSQLProcess(String query, String addressUser, LocalDateTime dateTime, Boolean isActive,
                                       String status, Map<Integer, SQLThreadInfo> sessionThreadMap,
                                       Integer sqlId, SQLThreadInfo sessionThread, Map<Integer, List<Object>> lockingMap) {

        Boolean fusionInTransaction = status  == null ? null : status.equals("idle in transaction");

        Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;
        boolean baseInTransaction = sessionThread != null && sessionThread.baseInTransaction;
        Long startTransaction = sessionThread == null ? null : sessionThread.startTransaction;
        String attemptCount = sessionThread == null ? "0" : sessionThread.attemptCount;
        Long user = sessionThread == null ? null : sessionThread.userActiveTask;
        Long computer = sessionThread == null ? null : sessionThread.computerActiveTask;
        String fullQuery = sessionThread == null ? null : sessionThread.fullQuery;
        boolean isDisabledNestLoop = sessionThread != null && sessionThread.isDisabledNestLoop;
        Integer queryTimeout = sessionThread == null ? null : sessionThread.queryTimeout;
        String debugInfo = sessionThread == null ? null : sessionThread.debugInfo;
        StatusMessage statusMessage = sessionThread == null ? null : sessionThread.statusMessage;

        List<Object> lockingProcess = lockingMap == null ? null : lockingMap.get(sqlId);
        Integer lockingSqlId = lockingProcess == null ? null : (Integer) lockingProcess.get(0);
        SQLThreadInfo lockingSessionThread = lockingSqlId == null ? null : sessionThreadMap.get(lockingSqlId);
        String lockOwnerId = lockingSessionThread == null ? null : getMonitorId(lockingSessionThread.javaThread, lockingSqlId);
        String lockOwnerName = lockingProcess == null ? null : (String) lockingProcess.get(1);

        LocalDateTime dateTimeCall = javaThread == null ? null : sqlTimestampToLocalDateTime(RemoteLoggerAspect.getDateTimeCall(javaThread.getId()));
        String threadName = sessionThread == null || sessionThread.javaThreadDebugInfo == null ? null : sessionThread.javaThreadDebugInfo.threadName;
        String threadStackTrace = sessionThread == null || sessionThread.javaThreadDebugInfo == null ? null : sessionThread.javaThreadDebugInfo.threadStackTrace;


        return new SQLProcess(dateTimeCall, query, fullQuery, user, computer, addressUser, dateTime,
                isActive, fusionInTransaction, baseInTransaction, startTransaction, attemptCount, status,
                statusMessage, lockOwnerId, lockOwnerName, sqlId, isDisabledNestLoop, queryTimeout, debugInfo, threadName, threadStackTrace);
    }

    private String getMonitorId(Thread javaThread, Integer processId) {
        return javaThread != null ? String.valueOf(javaThread.getId()) : ("s" + processId);
    }

    private DataClass processIDType = IntegerClass.instance;

    protected ImMap<String, SQLProcess> getMSSQLProcesses(ExecutionContext context, Map<Integer, SQLThreadInfo> sessionThreadMap,
                                                        MSet<Thread> javaThreads, MExclSet<String> mFreeSQLProcesses, boolean onlyActive)
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
        propertyReaders.exclAdd("start_time", DateTimeClass.dateTime);
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap()
                , 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<String, SQLProcess> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("text"));
            if (!query.equals(originalQuery) && (!onlyActive || !query.isEmpty())) {
                Integer processId = (Integer) entry.get("session_id");
                String address = trimToNull((String) entry.get("client_net_address"));
                LocalDateTime dateTime = (LocalDateTime) entry.get("start_time");

                SQLThreadInfo sessionThread = sessionThreadMap.get(processId);
                Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;

                String resultId = getMonitorId(javaThread, processId);

                if (!query.isEmpty()) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                SQLProcess newEntry = getSQLProcess(query, address, dateTime, null, null, sessionThreadMap, processId, sessionThread, null);
                SQLProcess prevEntry = resultMap.put(resultId, newEntry);

                if (prevEntry != null) {
                    LocalDateTime prevDateTime = prevEntry.dateTime;
                    if (prevDateTime != null && (dateTime == null || prevDateTime.compareTo(dateTime) > 0)) {
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

    protected ImMap<String, SQLProcess> getPostgresProcesses(ExecutionContext context, Map<Integer, SQLThreadInfo> sessionThreadMap,
                                                           MSet<Thread> javaThreads, MExclSet<String> mFreeSQLProcesses, boolean onlyActive,
                                                           boolean logSqlProcesses)
            throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Map<Integer, List<Object>> lockingMap = getPostgresLockMap(context);

        String originalQuery = String.format("SELECT * FROM pg_stat_activity WHERE datname='%s'" + (onlyActive ? " AND state!='idle'" : ""), context.getBL().getDataBaseName());

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
        propertyReaders.exclAdd("query_start", DateTimeClass.dateTime);
        propertyReaders.exclAdd("state", StringClass.get(100));
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        if (logSqlProcesses) {
            ServerLoggers.exInfoLogger.info("sessionThreadMap");
            for (Map.Entry<Integer, SQLThreadInfo> entry : sessionThreadMap.entrySet()) {
                Thread javaThread = entry.getValue().javaThread;
                ServerLoggers.exInfoLogger.info(String.format("SQL ID: %s, Java ID: %s, User: %s, Computer: %s",
                        entry.getKey(), javaThread != null ? javaThread.getId() : null, entry.getValue().userActiveTask, entry.getValue().computerActiveTask));
            }
        }

        Map<String, SQLProcess> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("query"));
            String state = trimToEmpty((String) entry.get("state"));

            if (logSqlProcesses)
                ServerLoggers.exInfoLogger.info(String.format("PID: %s, Address: %s, State: %s, Query: %s", entry.get("pid"), entry.get("client_addr"), state, query));

            boolean active = !query.isEmpty() && state.equals("active");
            if (!query.equals(originalQuery) && (!onlyActive || active)) {
                Integer sqlId = (Integer) entry.get("pid");
                String address = trimToNull((String) entry.get("client_addr"));
                LocalDateTime dateTime = (LocalDateTime) entry.get("query_start");

                SQLThreadInfo sessionThread = sessionThreadMap.get(sqlId);
                Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;

                String resultId = getMonitorId(javaThread, sqlId);

                if (active) {
                    if (javaThread != null)
                        javaThreads.add(javaThread);
                    else {
                        mFreeSQLProcesses.exclAdd(resultId);
                    }
                }

                SQLProcess newEntry = getSQLProcess(query, address, dateTime, active, state, sessionThreadMap, sqlId, sessionThread, lockingMap);
                SQLProcess prevEntry = resultMap.put(resultId, newEntry);

                if (prevEntry != null) {
                    LocalDateTime prevDateTime = prevEntry.dateTime;
                    if (prevDateTime != null && (dateTime == null || prevDateTime.compareTo(dateTime) > 0)) {
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

    private Map<Integer, List<Object>> getPostgresLockMap(ExecutionContext context) throws SQLException, SQLHandledException {
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
            resultMap.put(blocked_pid, Arrays.asList(blocking_pid, blocking_statement));
        }
        return resultMap;
    }

    private String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}