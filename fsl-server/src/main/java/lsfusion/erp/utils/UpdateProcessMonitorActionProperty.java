package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
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
import lsfusion.server.context.ThreadType;
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
import lsfusion.server.logics.service.process.JavaProcess;
import lsfusion.server.logics.service.process.SQLProcess;
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
            boolean readAllocatedBytes = Settings.get().isReadAllocatedBytes();
            String processType = trimToEmpty((String) findProperty("nameProcessType[]").read(context));
            context.getSession().cancel(context.stack, SetFact.singleton((SessionDataProperty) findProperty("processType[]").property));

            updateProcessMonitor(context, processType, readAllocatedBytes);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void updateProcessMonitor(ExecutionContext context, String processType, boolean readAllocatedBytes) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

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
                active || activeSQL ? sqlJavaActiveThreads : SetFact.<Thread>EMPTY(), active || activeJava, readAllocatedBytes, logSqlProcesses);

        // вырезаем "лишние" СУБД'ые процессы (которые нужны чисто чтобы видеть последние запросы)
        if (active) { // оставляем только javaProcesses + freeProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys().merge(freeSQLProcesses));
        }
        if (activeJava) { // оставляем javaProcesses
            sqlProcesses = sqlProcesses.filter(javaProcesses.keys());
        }

        ImOrderSet<LCP> propsJava = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "stackTraceJavaProcess[VARSTRING[10]]", "nameJavaProcess[VARSTRING[10]]",
                "statusJavaProcess[VARSTRING[10]]", "lockNameJavaProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]", "lockOwnerNameProcess[VARSTRING[10]]",
                "nameComputerJavaProcess[VARSTRING[10]]", "nameUserJavaProcess[VARSTRING[10]]", "lsfStackTraceProcess[VARSTRING[10]]",
                "threadAllocatedBytesProcess[VARSTRING[10]]", "lastThreadAllocatedBytesProcess[VARSTRING[10]]"));

        ImOrderSet<LCP> propsSQL = getProps(findProperties("idThreadProcess[VARSTRING[10]]", "dateTimeCallProcess[VARSTRING[10]]", "threadTypeProcess[VARSTRING[20]]",
                "querySQLProcess[VARSTRING[10]]", "addressUserSQLProcess[VARSTRING[10]]", "dateTimeSQLProcess[VARSTRING[10]]", "isActiveSQLProcess[VARSTRING[10]]",
                "inTransactionSQLProcess[VARSTRING[10]]", "startTransactionSQLProcess[VARSTRING[10]]", "attemptCountSQLProcess[VARSTRING[10]]", "statusSQLProcess[VARSTRING[10]]",
                "computerProcess[VARSTRING[10]]", "userProcess[VARSTRING[10]]", "lockOwnerIdProcess[VARSTRING[10]]", "lockOwnerNameProcess[VARSTRING[10]]", "fullQuerySQLProcess[VARSTRING[10]]",
                "idSQLProcess[VARSTRING[10]]", "isDisabledNestLoopProcess[VARSTRING[10]]", "queryTimeoutProcess[VARSTRING[10]]", "debugInfoSQLProcess[VARSTRING[10]]"));

        int rowsJava = writeRowsJava(context, propsJava, javaProcesses);
        int rowsSQL = writeRowsSQL(context, propsSQL, sqlProcesses);
        if (rowsJava == 0 && rowsSQL == 0)
            findAction("formRefresh[]").execute(context);

    }

    private GetValue<ObjectValue, LCP> getJavaMapValueGetter(final JavaProcess javaProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getJavaMapValue(prop, javaProcessValue, idThread);
            }
        };
    }

    private GetValue<ObjectValue, LCP> getSQLMapValueGetter(final SQLProcess sqlProcessValue, final String idThread) {
        return new GetValue<ObjectValue, LCP>() {
            public ObjectValue getMapValue(LCP prop) {
                return getSQLMapValue(prop, sqlProcessValue, idThread);
            }
        };
    }

    private ObjectValue getJavaMapValue(LCP prop, JavaProcess javaProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "stackTraceJavaProcess":
                return javaProcess.stackTrace == null ? NullValue.instance : new DataObject(javaProcess.stackTrace);
            case "nameJavaProcess":
                return javaProcess.name == null ? NullValue.instance : new DataObject(javaProcess.name);
            case "statusJavaProcess":
                return javaProcess.status == null ? NullValue.instance : new DataObject(javaProcess.status);
            case "lockNameJavaProcess":
                return javaProcess.lockName == null ? NullValue.instance : new DataObject(javaProcess.lockName);
            case "lockOwnerIdProcess":
                return javaProcess.lockOwnerId == null ? NullValue.instance : new DataObject(javaProcess.lockOwnerId);
            case "lockOwnerNameProcess":
                return javaProcess.lockOwnerName == null ? NullValue.instance : new DataObject(javaProcess.lockOwnerName);
            case "nameComputerJavaProcess":
                return javaProcess.computer == null ? NullValue.instance : new DataObject(javaProcess.computer);
            case "nameUserJavaProcess":
                return javaProcess.user == null ? NullValue.instance : new DataObject(javaProcess.user);
            case "lsfStackTraceProcess":
                return javaProcess.lsfStackTrace == null ? NullValue.instance : new DataObject(javaProcess.lsfStackTrace);
            case "threadAllocatedBytesProcess":
                return javaProcess.threadAllocatedBytes == null ? NullValue.instance : new DataObject(javaProcess.threadAllocatedBytes, LongClass.instance);
            case "lastThreadAllocatedBytesProcess":
                return javaProcess.lastThreadAllocatedBytes == null ? NullValue.instance : new DataObject(javaProcess.lastThreadAllocatedBytes, LongClass.instance);
            default:
                return NullValue.instance;
        }
    }

    private ObjectValue getSQLMapValue(LCP prop, SQLProcess sqlProcess, String idThread) {
        switch (prop.property.getName()) {
            case "idThreadProcess":
                return idThread == null ? NullValue.instance : new DataObject(idThread);
            case "dateTimeCallProcess":
                return sqlProcess.dateTimeCall == null ? NullValue.instance : new DataObject(sqlProcess.dateTimeCall, DateTimeClass.instance);
            case "threadTypeProcess":
                return sqlProcess.threadType == null ? NullValue.instance : new DataObject(String.valueOf(sqlProcess.threadType));
            case "querySQLProcess":
                return sqlProcess.query == null ? NullValue.instance : new DataObject(sqlProcess.query);
            case "fullQuerySQLProcess":
                return sqlProcess.fullQuery == null ? NullValue.instance : new DataObject(sqlProcess.fullQuery);
            case "userProcess":
                return sqlProcess.user == null ? NullValue.instance : new DataObject(sqlProcess.user);
            case "computerProcess":
                return sqlProcess.computer == null ? NullValue.instance : new DataObject(sqlProcess.computer);
            case "addressUserSQLProcess":
                return sqlProcess.addressUser == null ? NullValue.instance : new DataObject(sqlProcess.addressUser);
            case "dateTimeSQLProcess":
                return sqlProcess.dateTime == null ? NullValue.instance : new DataObject(sqlProcess.dateTime, DateTimeClass.instance);
            case "isActiveSQLProcess":
                return DataObject.create(sqlProcess.isActive != null && sqlProcess.isActive);
            case "inTransactionSQLProcess":
                if (sqlProcess.baseInTransaction != null && sqlProcess.fusionInTransaction != null && sqlProcess.fusionInTransaction)
                    ServerLoggers.assertLog(sqlProcess.baseInTransaction.equals(true), "FUSION AND BASE INTRANSACTION DIFFERS");
                return DataObject.create(sqlProcess.baseInTransaction != null ? sqlProcess.baseInTransaction : sqlProcess.fusionInTransaction);
            case "startTransactionSQLProcess":
                return sqlProcess.startTransaction == null ? NullValue.instance : new DataObject(new Timestamp(sqlProcess.startTransaction), DateTimeClass.instance);
            case "attemptCountSQLProcess":
                return sqlProcess.attemptCount == null ? NullValue.instance : new DataObject(sqlProcess.attemptCount);
            case "statusSQLProcess":
                return sqlProcess.status == null ? NullValue.instance : new DataObject(sqlProcess.status);
            case "lockOwnerIdProcess":
                return sqlProcess.lockOwnerId == null ? NullValue.instance : new DataObject(sqlProcess.lockOwnerId);
            case "lockOwnerNameProcess":
                return sqlProcess.lockOwnerName == null ? NullValue.instance : new DataObject(sqlProcess.lockOwnerName);
            case "idSQLProcess":
                return sqlProcess.sqlId == null ? NullValue.instance : new DataObject(sqlProcess.sqlId);
            case "isDisabledNestLoopProcess":
                return DataObject.create(sqlProcess.isDisabledNestLoop != null && sqlProcess.isDisabledNestLoop);
            case "queryTimeoutProcess":
                return sqlProcess.queryTimeout == null ? NullValue.instance : new DataObject(sqlProcess.queryTimeout);
            case "debugInfoSQLProcess":
                return sqlProcess.debugInfo == null ? NullValue.instance : new DataObject(sqlProcess.debugInfo);
            default:
                return NullValue.instance;
        }
    }

    private int writeRowsJava(ExecutionContext context, final ImOrderSet<LCP> props, ImMap<String, JavaProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> rows;

        rows = processes.mapKeyValues(new GetValue<ImMap<String, DataObject>, String>() {
            public ImMap<String, DataObject> getMapValue(String value) {
                return MapFact.singleton("key", new DataObject(value, StringClass.get(10)));
            }
        }, new GetKeyValue<ImMap<LCP, ObjectValue>, String, JavaProcess>() {
            public ImMap<LCP, ObjectValue> getMapValue(String key, JavaProcess value) {
                return props.getSet().mapValues(getJavaMapValueGetter(value, key));
            }
        });

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, new Type.Getter<LCP>() {
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

    private int writeRowsSQL(ExecutionContext context, final ImOrderSet<LCP> props, ImMap<String, SQLProcess> processes) throws SQLException, SQLHandledException {

        ImMap<ImMap<String, DataObject>, ImMap<LCP, ObjectValue>> rows;

        rows = processes.mapKeyValues(new GetValue<ImMap<String, DataObject>, String>() {
            public ImMap<String, DataObject> getMapValue(String value) {
                return MapFact.singleton("key", new DataObject(value, StringClass.get(10)));
            }
        }, new GetKeyValue<ImMap<LCP, ObjectValue>, String, SQLProcess>() {
            public ImMap<LCP, ObjectValue> getMapValue(String key, SQLProcess value) {
                return props.getSet().mapValues(getSQLMapValueGetter(value, key));
            }
        });

        SingleKeyTableUsage<LCP> importTable = new SingleKeyTableUsage<>("updpm:wr", StringClass.get(10), props, new Type.Getter<LCP>() {
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

    private ImMap<String, SQLProcess> getMSSQLProcesses(ExecutionContext context, Map<Integer, SQLThreadInfo> sessionThreadMap,
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

        Map<String, SQLProcess> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trimToEmpty((String) entry.get("text"));
            if (!query.equals(originalQuery) && (!onlyActive || !query.isEmpty())) {
                Integer processId = (Integer) entry.get("session_id");
                SQLThreadInfo sessionThread = sessionThreadMap.get(processId);
                String fullQuery = sessionThread == null ? null : sessionThread.fullQuery;
                boolean isDisabledNestLoop = sessionThread != null && sessionThread.isDisabledNestLoop;
                Integer queryTimeout = sessionThread == null ? null : sessionThread.queryTimeout;
                String debugInfo = sessionThread == null ? null : sessionThread.debugInfo;

                String address = trimToNull((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");

                Thread javaThread = sessionThread == null ? null : sessionThread.javaThread;
                boolean baseInTransaction = sessionThread != null && sessionThread.baseInTransaction;
                Long startTransaction = sessionThread == null ? null : sessionThread.startTransaction;
                String attemptCount = sessionThread == null ? "0" : sessionThread.attemptCount;

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
                        null, null, processId, isDisabledNestLoop, queryTimeout, debugInfo);
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
        propertyReaders.exclAdd("pid", IntegerClass.instance);
        propertyReaders.exclAdd("usename", StringClass.get(100));
        propertyReaders.exclAdd("client_addr", PGObjectReader.instance);
        propertyReaders.exclAdd("query_start", DateTimeClass.instance);
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
                String debugInfo = sessionThread == null ? null : sessionThread.debugInfo;

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

                SQLProcess newEntry = new SQLProcess(dateTimeCall, threadType, query, fullQuery, userActiveTask, computerActiveTask, address, dateTime,
                        active, state.equals("idle in transaction"), baseInTransaction, startTransaction, attemptCount, state,
                        lockOwnerId, lockOwnerName, sqlId, isDisabledNestLoop, queryTimeout, debugInfo);
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

    private ImMap<String, JavaProcess> getJavaProcesses(ImSet<Thread> allThreads, ImSet<Thread> sqlThreads, boolean onlyActive, boolean readAllocatedBytes, boolean logSqlProcesses) {
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
            JavaProcess threadInfo = getJavaProcess(thread, onlyActive && allThreads != null && !sqlThreads.contains(thread), threadInfos[i], allocatedBytes == null ? null : allocatedBytes[i]);
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