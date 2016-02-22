package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.LongClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
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
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.remote.RemoteLoggerAspect;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SingleKeyTableUsage;
import lsfusion.server.stack.ExecutionStackAspect;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static lsfusion.base.BaseUtils.trimToEmpty;
import static lsfusion.base.BaseUtils.trimToNull;

public class UpdateProcessMonitorActionProperty extends ScriptingActionProperty {

    public UpdateProcessMonitorActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            boolean readAllocatedBytes = findProperty("readAllocatedBytes[]").read(context) != null;
            String processType = trimToEmpty((String) findProperty("nameProcessType[]").read(context));
            context.getSession().cancel(SetFact.singleton((SessionDataProperty) findProperty("processType[]").property));

            updateProcessMonitor(context, processType, readAllocatedBytes);

            boolean baseReadAllocatedBytes = findProperty("readAllocatedBytes[]").read(context) != null;
            if (baseReadAllocatedBytes != readAllocatedBytes) {
                findProperty("readAllocatedBytes[]").change(readAllocatedBytes ? true : null, context);
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

        MSet<Thread> mSqlJavaActiveThreads = SetFact.mSet();
        MExclSet<String> mFreeSQLProcesses = SetFact.mExclSet();
        ImMap<String, List<Object>> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ?
                getPostgresProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL)
                : getMSSQLProcesses(context, sessionThreadMap, mSqlJavaActiveThreads, mFreeSQLProcesses, activeSQL);
        ImSet<Thread> sqlJavaActiveThreads = mSqlJavaActiveThreads.immutable();
        ImSet<String> freeSQLProcesses = mFreeSQLProcesses.immutable();

        ImMap<String, List<Object>> javaProcesses = getJavaProcesses(activeSQL ? null : ThreadUtils.getAllThreads(), active || activeSQL ? sqlJavaActiveThreads : SetFact.<Thread>EMPTY(), active || activeJava, readAllocatedBytes);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        if(active) { // оставляем только javaProcesses + freeProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));
        }
        if(activeJava) { // оставляем javaProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys());
        }

        ImOrderSet<LCP> propsJava = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "stackTraceJavaProcess[VARSTRING[10]]", "nameJavaProcess[VARSTRING[10]]", "statusJavaProcess[VARSTRING[10]]",
                "lockNameJavaProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]", "lockOwnerNameProcess[VARSTRING[10]]", "nameComputerJavaProcess[VARSTRING[10]]", "nameUserJavaProcess[VARSTRING[10]]", "lsfStackTraceProcess[VARSTRING[10]]",
                "threadAllocatedBytesProcess[VARSTRING[10]]", "lastThreadAllocatedBytesProcess[VARSTRING[10]]"));

        ImOrderSet<LCP> propsSQL = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "dateTimeCallProcess[VARSTRING[10]]", "querySQLProcess[VARSTRING[10]]", "addressUserSQLProcess[VARSTRING[10]]", "dateTimeSQLProcess[VARSTRING[10]]",
                "isActiveSQLProcess[VARSTRING[10]]", "inTransactionSQLProcess[VARSTRING[10]]", "startTransactionSQLProcess[VARSTRING[10]]", "attemptCountSQLProcess[VARSTRING[10]]", "statusMessageSQLProcess[VARSTRING[10]]",
                "computerProcess[VARSTRING[10]]", "userProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]", "lockOwnerNameProcess[VARSTRING[10]]", "fullQuerySQLProcess[VARSTRING[10]]", "idSQLProcess[VARSTRING[10]]",
                "isDisabledNestLoopProcess[VARSTRING[10]]", "queryTimeoutProcess[VARSTRING[10]]"));

        int rowsJava = writeRows(context, propsJava, javaProcesses, true);
        int rowsSQL = writeRows(context, propsSQL, sqlProcesses, false);
        if (rowsJava == 0 && rowsSQL == 0)
            findAction("formRefresh[]").execute(context);

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
            case "dateTimeCallProcess":
                Timestamp dateTimeCall = (Timestamp) sqlProcess.get(0);
                return dateTimeCall == null ? NullValue.instance : new DataObject(dateTimeCall, DateTimeClass.instance);
            case "querySQLProcess":
                String query = (String) sqlProcess.get(1);
                return query == null ? NullValue.instance : new DataObject(query);
            case "fullQuerySQLProcess":
                String fullQuery = (String) sqlProcess.get(2);
                return fullQuery == null ? NullValue.instance : new DataObject(fullQuery);
            case "userProcess":
                Integer user = (Integer) sqlProcess.get(3);
                return user == null ? NullValue.instance : new DataObject(user);
            case "computerProcess":
                Integer computer = (Integer) sqlProcess.get(4);
                return computer == null ? NullValue.instance : new DataObject(computer);
            case "addressUserSQLProcess":
                String address = (String) sqlProcess.get(5);
                return address == null ? NullValue.instance : new DataObject(address);
            case "dateTimeSQLProcess":
                Timestamp dateTime = (Timestamp) sqlProcess.get(6);
                return dateTime == null ? NullValue.instance : new DataObject(dateTime, DateTimeClass.instance);
            case "isActiveSQLProcess":
                Boolean isActive = (Boolean) sqlProcess.get(7);
                return isActive == null || !isActive ? NullValue.instance : new DataObject(true);
            case "inTransactionSQLProcess":
                Boolean fusionInTransaction = (Boolean) sqlProcess.get(8);
                Boolean baseInTransaction = (Boolean) sqlProcess.get(9);
                if(baseInTransaction != null && fusionInTransaction != null && fusionInTransaction)
                    ServerLoggers.assertLog(baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                Boolean inTransaction = baseInTransaction != null ? baseInTransaction : fusionInTransaction;
                return !inTransaction ? NullValue.instance : new DataObject(true);
            case "startTransactionSQLProcess":
                Long startTransaction = (Long) sqlProcess.get(10);
                return startTransaction == null ? NullValue.instance : new DataObject(new Timestamp(startTransaction), DateTimeClass.instance);
            case "attemptCountSQLProcess":
                String attemptCount = (String) sqlProcess.get(11);
                return attemptCount == null ? NullValue.instance : new DataObject(attemptCount);
            case "statusMessageSQLProcess":
                StatusMessage statusMessage = (StatusMessage) sqlProcess.get(12);
                return statusMessage == null ? NullValue.instance : new DataObject(statusMessage.getMessage());
            case "lockOwnerIdProcess":
                String lockOwnerId = (String) sqlProcess.get(13);
                return lockOwnerId == null ? NullValue.instance : new DataObject(lockOwnerId);
            case "lockOwnerNameProcess":
                String lockOwnerName = (String) sqlProcess.get(14);
                return lockOwnerName == null ? NullValue.instance : new DataObject(lockOwnerName);
            case "idSQLProcess":
                Integer idSQLProcess = (Integer) sqlProcess.get(15);
                return idSQLProcess == null ? NullValue.instance : new DataObject(idSQLProcess);
            case "isDisabledNestLoopProcess":
                Boolean isDisabledNestLoop = (Boolean) sqlProcess.get(16);
                return isDisabledNestLoop == null || !isDisabledNestLoop ? NullValue.instance : new DataObject(true);
            case "queryTimeoutProcess":
                Integer queryTimeoutProcess = (Integer) sqlProcess.get(17);
                return queryTimeoutProcess == null ? NullValue.instance : new DataObject(queryTimeoutProcess);
            default:
                return NullValue.instance;
        }
    }

    private int writeRows(ExecutionContext context, final ImOrderSet<LCP> props, ImMap<String, List<Object>> processes, final boolean java) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> rows = processes.mapKeyValues(new GetValue<ImMap<String, DataObject>, String>() {
            public ImMap<String, DataObject> getMapValue(String value) {
                return MapFact.singleton("key", new DataObject(value));
            }
        }, new GetKeyValue<ImMap<LCP, ObjectValue>, String, List<Object>>() {
            public ImMap<LCP, ObjectValue> getMapValue(String key, List<Object> value) {
                return props.getSet().mapValues(java ? getJavaMapValueGetter(value, key) : getSQLMapValueGetter(value, key));
            }
        });

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>(StringClass.get(10), props, new Type.Getter<LCP>() {
            @Override
            public Type getType(LCP key) {
                return key.property.getType();
            }
        });
        OperationOwner owner = context.getSession().getOwner();
        SQLSession sql = context.getSession().sql;
        importTable.writeRows(sql, rows, owner);

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
        return rows.size();
    }

    private ImOrderSet<LCP> getProps(LCP<?>[] properties) {
        return SetFact.fromJavaOrderSet(new ArrayList<LCP>(Arrays.asList(properties))).addOrderExcl(LM.baseLM.importedString);
    }

    private ImMap<String, List<Object>> getMSSQLProcesses(ExecutionContext context, Map<Integer, List<Object>> sessionThreadMap,
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
            if(!query.equals(originalQuery) && (!onlyActive || !query.isEmpty())) {
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
                //String userActiveTask = trimToNull((String) entry.get("host_name"));
                String address = trimToNull((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");

                Thread javaThread = sessionThread == null ? null : (Thread) sessionThread.get(0);
                boolean baseInTransaction = sessionThread != null && (boolean) sessionThread.get(1);
                Long startTransaction = sessionThread == null ? null : (Long) sessionThread.get(2);
                String attemptCount = sessionThread == null ? "0" : (String) sessionThread.get(3);
                StatusMessage statusMessage = sessionThread == null ? null : (StatusMessage) sessionThread.get(4);

                String resultId = getMonitorId(javaThread, processId);

                if(!query.isEmpty()) {
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
                                                             MSet<Thread> javaThreads, MExclSet<String> mFreeSQLProcesses, boolean onlyActive)
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

        MMap<String, List<Object>> mResultMap = MapFact.mMap(MapFact.<String, List<Object>>override());
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("query"));
            String state = trimToEmpty((String) entry.get("state"));
            boolean active = !query.isEmpty() && state.equals("active");
            if (!query.equals(originalQuery) && (!onlyActive || active)) {
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
                Integer lockingSqlId = lockingProcess == null ? null : (Integer)lockingProcess.get(0);
                List<Object> lockingSessionThread = lockingSqlId == null ? null : sessionThreadMap.get(lockingSqlId);
                String lockOwnerId = lockingSessionThread == null ? null : getMonitorId((Thread)lockingSessionThread.get(0), lockingSqlId);
                String lockOwnerName = lockingProcess == null ? null : (String) lockingProcess.get(1);

                String resultId = getMonitorId(javaThread, sqlId);

                if(active) {
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

    private ImMap<String, List<Object>> getJavaProcesses(ImSet<Thread> allThreads, ImSet<Thread> sqlThreads, boolean onlyActive, boolean readAllocatedBytes) {
        ImSet<Thread> threads;
        if(allThreads != null) {
            threads = allThreads;
        } else {
            threads = sqlThreads;
        }

        long[] threadIds = new long[threads.size()];
        for(int i=0,size=threads.size();i<size;i++) {
            threadIds[i] = threads.get(i).getId();
        }
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos;
        if(Settings.get().isUseSafeMonitorProcess()) {
            threadInfos = new ThreadInfo[threadIds.length];
            for(int i=0;i<threadInfos.length;i++)
                threadInfos[i] = tBean.getThreadInfo(threadIds[i], Integer.MAX_VALUE);
        } else
            threadInfos = tBean.getThreadInfo(threadIds, Integer.MAX_VALUE);

        long[] allocatedBytes = null;
        if (readAllocatedBytes && tBean instanceof com.sun.management.ThreadMXBean) {
            if(Settings.get().isUseSafeMonitorProcess()) {
//                allocatedBytes = new long[threadIds.length];
//                for (int i = 0; i < threadInfos.length; i++)
//                    allocatedBytes[i] = ((com.sun.management.ThreadMXBean) tBean).getThreadAllocatedBytes(threadIds[i]);
            } else
                allocatedBytes = ((com.sun.management.ThreadMXBean) tBean).getThreadAllocatedBytes(threadIds);
        }

        MExclMap<String, List<Object>> mResultMap = MapFact.mExclMap();
        for(int i=0,size=threads.size();i<size;i++) {
            Thread thread = threads.get(i);
            List<Object> threadInfo = getThreadInfo(thread, onlyActive && allThreads != null && !sqlThreads.contains(thread), threadInfos[i], allocatedBytes == null ? null : allocatedBytes[i]);
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
        String stackTrace = threadInfo == null ? null : getJavaStack(threadInfo.getStackTrace());
        String name = threadInfo == null ? null : threadInfo.getThreadName();
        String lockName = threadInfo == null ? null : threadInfo.getLockName();
        String lockOwnerId = threadInfo == null ? null : String.valueOf(threadInfo.getLockOwnerId());
        String lockOwnerName = threadInfo == null ? null : threadInfo.getLockOwnerName();
        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
        String computer = logInfo == null ? null : logInfo.hostnameComputer;
        String user = logInfo == null ? null : logInfo.userName;
        String lsfStack = getLSFStack(thread);
        Long lastAllocatedBytes = SQLSession.getThreadAllocatedBytes(allocatedBytes, id);

        return !onlyActive || isActiveJavaProcess(status, stackTrace) ? Arrays.asList((Object) stackTrace, name, status, lockName, lockOwnerId,
                lockOwnerName, computer, user, lsfStack, allocatedBytes, lastAllocatedBytes) : null;
    }

    private boolean isActiveJavaProcess(String status, String stackTrace) {
        return status != null && (status.equals("RUNNABLE") || status.equals("BLOCKED")) && (stackTrace != null
                && !stackTrace.startsWith("java.net.DualStackPlainSocketImpl")
                && !stackTrace.startsWith("sun.awt.windows.WToolkit.eventLoop")
                && !stackTrace.startsWith("java.net.SocketInputStream.socketRead0")
                && !stackTrace.startsWith("sun.management.ThreadImpl.dumpThreads0")
                && !stackTrace.startsWith("java.net.SocketOutputStream.socketWrite")
                && !stackTrace.startsWith("java.net.PlainSocketImpl")
                && !stackTrace.startsWith("java.io.FileInputStream.readBytes")
                && !stackTrace.startsWith("java.lang.UNIXProcess.waitForProcessExit"))
                && !stackTrace.contains("UpdateProcessMonitor");
    }

    private String getLSFStack(Thread thread) {
        try {
            return thread == null ? null : ExecutionStackAspect.getStackString(thread, true, true);
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

    protected String trim(String input, Integer length) {
        return input == null ? null : (length == null || length >= input.trim().length() ? input.trim() : input.trim().substring(0, length));
    }
}