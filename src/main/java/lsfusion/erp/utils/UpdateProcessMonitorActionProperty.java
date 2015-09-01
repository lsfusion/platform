package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.LongClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SingleKeyTableUsage;
import lsfusion.server.stack.ExecutionStackAspect;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class UpdateProcessMonitorActionProperty extends ScriptingActionProperty {

    public UpdateProcessMonitorActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = findProperty("readAllocatedBytes").read(context) != null;
            String processType = trimToEmpty((String) findProperty("nameProcessType").read(context));
            context.getSession().cancel(SetFact.singleton((SessionDataProperty) findProperty("processType").property));

            updateProcessMonitor(context, processType, readAllocatedBytes);

            try (DataSession session = context.createSession()) {
                findProperty("readAllocatedBytes").change(readAllocatedBytes ? true : null, session);
                session.apply(context.getBL());
            }

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void updateProcessMonitor(ExecutionContext context, String processType, boolean readAllocatedBytes) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

        boolean active = processType.endsWith("activeAll");
        boolean activeSQL = processType.isEmpty() || processType.endsWith("activeSQL");
        boolean activeJava = processType.endsWith("activeJava");

        Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();
        List<Long> idSet = new ArrayList<>();

        Map<String, List<Object>> javaProcesses = new HashMap<>();
        Map<String, List<Object>> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, javaProcesses, idSet, active || activeSQL, activeJava, readAllocatedBytes)
                : getMSSQLProcesses(context, sessionThreadMap, javaProcesses, idSet, active || activeSQL, activeJava, readAllocatedBytes);
        if(!activeSQL)
            javaProcesses.putAll(getJavaProcesses(idSet, active || activeJava, readAllocatedBytes));

        ImOrderSet<LCP> propsJava = getProps(findProperties("idThreadProcess", "stackTraceJavaProcess", "nameJavaProcess", "statusJavaProcess",
                "lockNameJavaProcess", "lockOwnerIdProcess", "lockOwnerNameProcess", "nameComputerJavaProcess", "nameUserJavaProcess", "lsfStackTraceProcess",
                "threadAllocatedBytesProcess", "lastThreadAllocatedBytesProcess"));

        ImOrderSet<LCP> propsSQL = getProps(findProperties("idThreadProcess", "querySQLProcess", "addressUserSQLProcess", "dateTimeSQLProcess",
                "isActiveSQLProcess", "inTransactionSQLProcess", "computerProcess", "userProcess", "lockOwnerIdProcess", "lockOwnerNameProcess",
                "fullQuerySQLProcess", "idSQLProcess", "isDisabledNestLoopProcess", "queryTimeoutProcess"));

        int rowsJava = writeRows(context, propsJava, javaProcesses, true);
        int rowsSQL = writeRows(context, propsSQL, sqlProcesses, false);
        if (rowsJava == 0 && rowsSQL == 0)
            findAction("formRefresh").execute(context);

    }

    private GetValue<ObjectValue, LCP> getJavaMapValueGetter(final List<Object> javaProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getJavaMapValue(prop, javaProcessValue, idThread);
            }
        };
    }

    private GetValue<ObjectValue, LCP> getSQLMapValueGetter(final List<Object> sqlProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getSQLMapValue(prop, sqlProcessValue, idThread);
            }
        };
    }

    private ObjectValue getJavaMapValue(LCP prop, List<Object> javaProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "stackTraceJavaProcess":
                String stackTrace = (String) javaProcess.get(0);
                return stackTrace == null ? NullValue.instance : new DataObject(stackTrace);
            case "nameJavaProcess":
                String name = (String) javaProcess.get(1);
                return name == null ? NullValue.instance : new DataObject(name);
            case "statusJavaProcess":
                String status = (String) javaProcess.get(2);
                return status == null ? NullValue.instance : new DataObject(status);
            case "lockNameJavaProcess":
                String lockName = (String) javaProcess.get(3);
                return lockName == null ? NullValue.instance : new DataObject(lockName);
            case "lockOwnerIdProcess":
                String lockOwnerId = (String) javaProcess.get(4);
                return lockOwnerId == null ? NullValue.instance : new DataObject(lockOwnerId);
            case "lockOwnerNameProcess":
                String lockOwnerName = (String) javaProcess.get(5);
                return lockOwnerName == null ? NullValue.instance : new DataObject(lockOwnerName);
            case "nameComputerJavaProcess":
                String computer = (String) javaProcess.get(6);
                return computer == null ? NullValue.instance : new DataObject(computer);
            case "nameUserJavaProcess":
                String user = (String) javaProcess.get(7);
                return user == null ? NullValue.instance : new DataObject(user);
            case "lsfStackTraceProcess":
                String lsfStackTrace = (String) javaProcess.get(8);
                return lsfStackTrace == null ? NullValue.instance : new DataObject(lsfStackTrace);
            case "threadAllocatedBytesProcess":
                Long threadAllocatedBytes = (Long) javaProcess.get(9);
                return threadAllocatedBytes == null ? NullValue.instance : new DataObject(threadAllocatedBytes, LongClass.instance);
            case "lastThreadAllocatedBytesProcess":
                Long lastThreadAllocatedBytes = (Long) javaProcess.get(10);
                return lastThreadAllocatedBytes == null ? NullValue.instance : new DataObject(lastThreadAllocatedBytes, LongClass.instance);
            default:
                return NullValue.instance;
        }
    }

    private ObjectValue getSQLMapValue(LCP prop, List<Object> sqlProcess, String idThread/*, Boolean baseInTransaction*/) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "querySQLProcess":
                String query = (String) sqlProcess.get(0);
                return query == null ? NullValue.instance : new DataObject(query);
            case "fullQuerySQLProcess":
                String fullQuery = (String) sqlProcess.get(1);
                return fullQuery == null ? NullValue.instance : new DataObject(fullQuery);
            case "userProcess":
                Integer user = (Integer) sqlProcess.get(2);
                return user == null ? NullValue.instance : new DataObject(user);
            case "computerProcess":
                Integer computer = (Integer) sqlProcess.get(3);
                return computer == null ? NullValue.instance : new DataObject(computer);
            case "addressUserSQLProcess":
                String address = (String) sqlProcess.get(4);
                return address == null ? NullValue.instance : new DataObject(address);
            case "dateTimeSQLProcess":
                Timestamp dateTime = (Timestamp) sqlProcess.get(5);
                return dateTime == null ? NullValue.instance : new DataObject(dateTime, DateTimeClass.instance);
            case "isActiveSQLProcess":
                Boolean isActive = (Boolean) sqlProcess.get(6);
                return isActive == null || !isActive ? NullValue.instance : new DataObject(true);
            case "inTransactionSQLProcess":
                Boolean fusionInTransaction = (Boolean) sqlProcess.get(7);
                Boolean baseInTransaction = (Boolean) sqlProcess.get(8);
                if(baseInTransaction != null && fusionInTransaction != null && fusionInTransaction)
                    ServerLoggers.assertLog(baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                Boolean inTransaction = baseInTransaction != null ? baseInTransaction : fusionInTransaction;
                return !inTransaction ? NullValue.instance : new DataObject(true);
            case "lockOwnerIdProcess":
                String lockOwnerId = (String) sqlProcess.get(9);
                return lockOwnerId == null ? NullValue.instance : new DataObject(lockOwnerId);
            case "lockOwnerNameProcess":
                String lockOwnerName = (String) sqlProcess.get(10);
                return lockOwnerName == null ? NullValue.instance : new DataObject(lockOwnerName);
            case "idSQLProcess":
                Integer idSQLProcess = (Integer) sqlProcess.get(11);
                return idSQLProcess == null ? NullValue.instance : new DataObject(idSQLProcess);
            case "isDisabledNestLoopProcess":
                Boolean isDisabledNestLoop = (Boolean) sqlProcess.get(12);
                return isDisabledNestLoop == null || !isDisabledNestLoop ? NullValue.instance : new DataObject(true);
            case "queryTimeoutProcess":
                Integer queryTimeoutProcess = (Integer) sqlProcess.get(13);
                return queryTimeoutProcess == null ? NullValue.instance : new DataObject(queryTimeoutProcess);
            default:
                return NullValue.instance;
        }
    }

    private int writeRows(ExecutionContext context, ImOrderSet<LCP> props, Map<String, List<Object>> processes, boolean java) throws SQLException, SQLHandledException {

        MExclMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> mRows = MapFact.mExclMap();
        for (final Map.Entry<String, List<Object>> process : processes.entrySet()) {
            if (process.getValue() != null) {
                DataObject rowKey = new DataObject(process.getKey());
                mRows.exclAdd(MapFact.singleton("key", rowKey), props.getSet().
                        mapValues(java ? getJavaMapValueGetter(process.getValue(), process.getKey()) : getSQLMapValueGetter(process.getValue(), process.getKey())));
            }
        }

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>(StringClass.get(10), props, new Type.Getter<LCP>() {
            @Override
            public Type getType(LCP key) {
                return key.property.getType();
            }
        });
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, mRows.immutable(), owner);

        ImRevMap<String, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<LCP> importJoin = importTable.join(mapKeys);
        Where where = importJoin.getWhere();
        try {
            for (LCP lcp : props) {
                PropertyChange propChange = new PropertyChange(MapFact.singletonRev(lcp.listInterfaces.single(), mapKeys.singleValue()), importJoin.getExpr(lcp), where);
                context.getEnv().change((CalcProperty) lcp.property, propChange);
            }
        } finally {
            importTable.drop(sql, owner);
        }
        return mRows.size();
    }

    private ImOrderSet<LCP> getProps(LCP<?>[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<LCP>(Arrays.asList(properties))).addOrderExcl(LM.baseLM.importedString);
    }

    private Map<String, List<Object>> getMSSQLProcesses(ExecutionContext context, Map<Integer, List<Object>> sessionThreadMap,
                                                        Map<String, List<Object>> javaProcesses, List<Long> idSet, boolean onlyActive, boolean onlyJava, boolean readAllocatedBytes)
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

        Map<String, List<Object>> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("text"));
            if(!query.equals(originalQuery) && (!onlyActive || !query.isEmpty())) {
                String fullQuery = null;
                boolean isDisabledNestLoop = false;
                Integer queryTimeout = null;
                Integer processId = (Integer) entry.get("session_id");
                List<Object> sessionThread = sessionThreadMap.get(processId);
                if (sessionThread != null) {
                    if (sessionThread.get(4) != null) {
                        fullQuery = (String) sessionThread.get(4);
                    }
                    isDisabledNestLoop = (boolean) sessionThread.get(5);
                    queryTimeout = (Integer) sessionThread.get(6);
                }
                //String userActiveTask = trimToNull((String) entry.get("host_name"));
                String address = trimToNull((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");

                Integer pid = sessionThread == null ? null : (Integer) sessionThread.get(0);
                boolean baseInTransaction = sessionThread != null && (boolean) sessionThread.get(1);
                boolean skip = onlyJava;
                if(pid != null){
                    List<Object> threadInfo = getThreadInfo(pid, onlyActive, false, readAllocatedBytes);
                    if(threadInfo != null) {
                        javaProcesses.put(String.valueOf(pid), threadInfo);
                        idSet.add((long) pid);
                        skip = false;
                    }
                }
                if (!skip)
                    resultMap.put(getSQLThreadId(sessionThread, processId), Arrays.asList((Object) query, fullQuery, null/*userActiveTask*/, null,
                            address, dateTime, null, null, baseInTransaction, null, null, processId, isDisabledNestLoop, queryTimeout));
            }
        }
        return resultMap;
    }

    private Map<String, List<Object>> getPostgresProcesses(ExecutionContext context, Map<Integer, List<Object>> sessionThreadMap,
                                                           Map<String, List<Object>> javaProcesses, List<Long> idSet, boolean onlyActive, boolean onlyJava, boolean readAllocatedBytes)
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
        propertyReaders.exclAdd("pid", IntegerClass.instance);
        propertyReaders.exclAdd("usename", StringClass.get(100));
        propertyReaders.exclAdd("client_addr", PGObjectReader.instance);
        propertyReaders.exclAdd("query_start", DateTimeClass.instance);
        propertyReaders.exclAdd("state", StringClass.get(100));
        propertyReaders.immutable();

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<String, List<Object>> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("query"));
            String state = trimToEmpty((String) entry.get("state"));
            boolean active = state.equals("active");
            if (!query.equals(originalQuery) && (!onlyActive || (!query.isEmpty() && active))) {
                Integer processId = (Integer) entry.get("pid");
                String address = trimToNull((String) entry.get("client_addr"));
                Timestamp dateTime = (Timestamp) entry.get("query_start");

                List<Object> sessionThread = sessionThreadMap.get(processId);
                Integer pid = sessionThread == null ? null : (Integer) sessionThread.get(0);
                boolean baseInTransaction = sessionThread != null && (boolean) sessionThread.get(1);
                Integer userActiveTask = sessionThread == null ? null : (Integer) sessionThread.get(2);
                Integer computerActiveTask = sessionThread == null ? null : (Integer) sessionThread.get(3);
                String fullQuery = sessionThread == null || sessionThread.get(4) == null ? null : (String) sessionThread.get(4);
                boolean isDisabledNestLoop = sessionThread != null && (boolean) sessionThread.get(5);
                Integer queryTimeout = sessionThread == null ? null : (Integer) sessionThread.get(6);

                List<Object> lockingProcess = lockingMap.get(processId);
                String lockOwnerId = lockingProcess == null ? null : getSQLThreadId(sessionThreadMap.get(lockingProcess.get(0)), (Integer) lockingProcess.get(0));
                String lockOwnerName = lockingProcess == null ? null : (String) lockingProcess.get(1);

                boolean skip = onlyJava;
                if(pid != null){
                    idSet.add((long) pid);
                    List<Object> threadInfo = getThreadInfo(pid, true, false, readAllocatedBytes);
                    if(threadInfo != null) {
                        javaProcesses.put(String.valueOf(pid), threadInfo);
                        skip = false;
                    }
                }
                if(!skip)
                resultMap.put(getSQLThreadId(sessionThread, processId), Arrays.<Object>asList(query, fullQuery, userActiveTask, computerActiveTask, address, dateTime,
                        active, state.equals("idle in transaction"), baseInTransaction, lockOwnerId, lockOwnerName, processId, isDisabledNestLoop, queryTimeout));
            }
        }
        return resultMap;
    }

    private Map<Integer, List<Object>> getPostgresLockMap(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
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

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

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

    private String getSQLThreadId(List<Object> sessionThread, Integer processId) {
        return sessionThread != null && sessionThread.get(0) != null ? String.valueOf(sessionThread.get(0)) : ("s" + processId);
    }

    private Map<String, List<Object>> getJavaProcesses(List<Long> idSet, boolean onlyActive, boolean readAllocatedBytes) {
        Map<String, List<Object>> resultMap = new HashMap<>();
        ThreadInfo[] threadInfoList = ManagementFactory.getThreadMXBean().dumpAllThreads(true, false);
        for (ThreadInfo thread : threadInfoList) {
            long pid = thread.getThreadId();
            if (!idSet.contains(pid)) {
                List<Object> threadInfo = getThreadInfo(pid, onlyActive, true, readAllocatedBytes);
                if (threadInfo != null)
                    resultMap.put(String.valueOf(pid), threadInfo);
            }
        }
        return resultMap;
    }

    private List<Object> getThreadInfo(long id, boolean onlyActive, boolean checkStackTrace, boolean readAllocatedBytes) {
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = tBean.getThreadInfo(id, Integer.MAX_VALUE);
        if(threadInfo == null) return null;

        Thread thread = getThreadById((int) id);
        String status = String.valueOf(threadInfo.getThreadState());
        String stackTrace = getJavaStack(threadInfo.getStackTrace());
        String name = threadInfo.getThreadName();
        String lockName = threadInfo.getLockName();
        String lockOwnerId = String.valueOf(threadInfo.getLockOwnerId());
        String lockOwnerName = threadInfo.getLockOwnerName();
        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
        String computer = logInfo == null ? null : logInfo.hostnameComputer;
        String user = logInfo == null ? null : logInfo.userName;
        String lsfStack = getLSFStack(thread);
        Long allocatedBytes = getThreadAllocatedBytes(tBean, readAllocatedBytes, id);
        Long lastAllocatedBytes = SQLSession.getThreadAllocatedBytes(id);

        return !onlyActive || isActiveJavaProcess(status, stackTrace, checkStackTrace) ? Arrays.asList((Object) stackTrace, name, status, lockName, lockOwnerId,
                lockOwnerName, computer, user, lsfStack, allocatedBytes, lastAllocatedBytes) : null;
    }

    private boolean isActiveJavaProcess(String status, String stackTrace, boolean checkStackTrace) {
        return status != null && (status.equals("RUNNABLE") || status.equals("BLOCKED")) && (!checkStackTrace || (stackTrace != null
                && !stackTrace.startsWith("java.net.DualStackPlainSocketImpl")
                && !stackTrace.startsWith("sun.awt.windows.WToolkit.eventLoop")
                && !stackTrace.startsWith("java.net.SocketInputStream.socketRead0")
                && !stackTrace.startsWith("sun.management.ThreadImpl.dumpThreads0")
                && !stackTrace.startsWith("java.net.SocketOutputStream.socketWrite")
                && !stackTrace.startsWith("java.net.PlainSocketImpl")
                && !stackTrace.startsWith("java.io.FileInputStream.readBytes")
                && !stackTrace.startsWith("java.lang.UNIXProcess.waitForProcessExit"))
                && !stackTrace.contains("UpdateProcessMonitor"));
    }

    private Thread getThreadById(int id) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    private String getLSFStack(Thread thread) {
        try {
            String lsfStack = thread == null ? null : ExecutionStackAspect.getStackString(thread, true);
            return lsfStack == null ? null : lsfStack.substring(0, Math.min(lsfStack.length(), 10000));
        } catch (Exception e) {
            return null;
        }
    }

    private String getJavaStack(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        String result = sb.toString();
        return result.isEmpty() ? null : result;
    }

    private Long getThreadAllocatedBytes(ThreadMXBean tBean, boolean readAllocatedBytes, long id) {
        Long allocatedBytes = null;
        if (readAllocatedBytes && tBean instanceof com.sun.management.ThreadMXBean) {
            allocatedBytes = ((com.sun.management.ThreadMXBean) tBean).getThreadAllocatedBytes(id);
        }
        return allocatedBytes;
    }

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}