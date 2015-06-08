package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateProcessMonitorActionProperty extends ScriptingActionProperty {

    public UpdateProcessMonitorActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            
            updateProcessMonitor(context);

        } catch (SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    protected void updateProcessMonitor(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        DataSession session = context.getSession();

        Integer previousCount = (Integer) findProperty("previousCountProcess").read(session);
        previousCount = previousCount == null ? 0 : previousCount;

        for (int i = 0; i < previousCount; i++) {
            DataObject currentObject = new DataObject(i);
            findProperty("previousCountProcess").change((Object) null, session, currentObject);

            findProperty("idThreadProcess").change((Object) null, session, currentObject);
            findProperty("computerProcess").change((Object) null, session, currentObject);
            findProperty("userProcess").change((Object) null, session, currentObject);

            findProperty("querySQLProcess").change((Object) null, session, currentObject);
            findProperty("addressUserSQLProcess").change((Object) null, session, currentObject);
            findProperty("dateTimeSQLProcess").change((Object) null, session, currentObject);
            findProperty("stateSQLProcess").change((Object) null, session, currentObject);

            findProperty("stackTraceJavaProcess").change((Object) null, session, currentObject);
            findProperty("nameJavaProcess").change((Object) null, session, currentObject);
            findProperty("statusJavaProcess").change((Object) null, session, currentObject);
            findProperty("lockNameJavaProcess").change((Object) null, session, currentObject);
            findProperty("lockOwnerIdJavaProcess").change((Object) null, session, currentObject);
            findProperty("lockOwnerNameJavaProcess").change((Object) null, session, currentObject);
        }

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

        Map<Integer, SQLSession> sessionMap = SQLSession.getSQLSessionMap();
        Map<String, List<Object>> javaProcesses = getActiveJavaThreads();
        Map<String, List<Object>> sqlProcesses = syntaxType == SQLSyntaxType.POSTGRES ? getPostgresProcesses(context) : getMSSQLProcesses(context);
        int i = 0;

        for (SQLSession sessionEntry : sessionMap.values()) {

            if(sessionEntry.getActiveThread() != null) {
                DataObject currentObject = new DataObject(i);
                String threadId = String.valueOf(sessionEntry.getActiveThread());

                findProperty("idThreadProcess").change(threadId, session, currentObject);

                List<Object> sqlProcess = sqlProcesses.get(threadId);
                if (sqlProcess != null)
                    writeSQLProcess(session, currentObject, sqlProcess);
                sqlProcesses.remove(threadId);

                List<Object> javaProcess = javaProcesses.get(threadId);
                if (javaProcess != null)
                    writeJavaProcess(session, currentObject, javaProcess, true);
                javaProcesses.remove(threadId);

                i++;
            }
        }

        for(Map.Entry<String, List<Object>> sqlProcess : sqlProcesses.entrySet()) {
            if (sqlProcess.getValue() != null) {
                DataObject currentObject = new DataObject(i);
                findProperty("idThreadProcess").change(sqlProcess.getKey(), session, currentObject);
                writeSQLProcess(session, currentObject, sqlProcess.getValue());
                i++;
            }
        }

        for(Map.Entry<String, List<Object>> javaProcess : javaProcesses.entrySet()) {
            if(javaProcess.getValue() != null) {
                DataObject currentObject = new DataObject(i);
                findProperty("idThreadProcess").change(javaProcess.getKey(), session, currentObject);
                writeJavaProcess(session, currentObject, javaProcess.getValue(), false);
                i++;
            }
        }

        findProperty("previousCountProcess").change(i, session);
    }

    private void writeSQLProcess(DataSession session, DataObject currentObject, List<Object> sqlProcess)
            throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        String query = trim((String) sqlProcess.get(0));
        String userActiveTask = trim((String) sqlProcess.get(1));
        String computerActiveTask = trim((String) sqlProcess.get(2));
        String address = trim((String) sqlProcess.get(3));
        Timestamp dateTime = (Timestamp) sqlProcess.get(4);
        DataObject state = (DataObject) sqlProcess.get(5);

        findProperty("computerProcess").change(computerActiveTask, session, currentObject);
        findProperty("userProcess").change(userActiveTask, session, currentObject);

        findProperty("querySQLProcess").change(query, session, currentObject);
        findProperty("addressUserSQLProcess").change(address, session, currentObject);
        findProperty("dateTimeSQLProcess").change(dateTime, session, currentObject);
        if(state != null)
            findProperty("stateSQLProcess").change(state.object, session, currentObject);
    }

    private void writeJavaProcess(DataSession session, DataObject currentObject, List<Object> javaProcess, boolean skipUser)
            throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        String stackTrace = (String) javaProcess.get(0);
        String name = (String) javaProcess.get(1);
        String status = (String) javaProcess.get(2);
        String lockName = (String) javaProcess.get(3);
        String lockOwnerId = (String) javaProcess.get(4);
        String lockOwnerName = (String) javaProcess.get(5);
        String computer = (String) javaProcess.get(6);
        String user = (String) javaProcess.get(7);

        if(!skipUser) {
            findProperty("computerProcess").change(computer, session, currentObject);
            findProperty("userProcess").change(user, session, currentObject);
        }

        findProperty("stackTraceJavaProcess").change(stackTrace, session, currentObject);
        findProperty("nameJavaProcess").change(name, session, currentObject);
        findProperty("statusJavaProcess").change(status, session, currentObject);
        findProperty("lockNameJavaProcess").change(lockName, session, currentObject);
        findProperty("lockOwnerIdJavaProcess").change(lockOwnerId, session, currentObject);
        findProperty("lockOwnerNameJavaProcess").change(lockOwnerName, session, currentObject);
    }

    private Map<String, List<Object>> getMSSQLProcesses(ExecutionContext context) throws SQLException, SQLHandledException {
        Map<Integer, SQLSession> sessionMap = SQLSession.getSQLSessionMap();
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

        ImOrderMap rs = context.getSession().sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                DynamicExecuteEnvironment.DEFAULT, 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<String, List<Object>> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trim((String) entry.get("text"));
            Integer processId = (Integer) entry.get("session_id");
            SQLSession sqlSession = sessionMap.get(processId);
            String userActiveTask = trim((String) entry.get("host_name"));
            String address = trim((String) entry.get("client_net_address"));
            Timestamp dateTime = (Timestamp) entry.get("start_time");

            if (!query.equals(originalQuery)) {
                resultMap.put(getSQLThreadId(sqlSession, processId), Arrays.asList((Object) query, userActiveTask, null, address, dateTime, null));
            }
        }
        return resultMap;
    }

    private Map<String, List<Object>> getPostgresProcesses(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        String originalQuery = String.format("SELECT * FROM pg_stat_activity WHERE datname='%s'"/* + (onlyActive ? " AND state!='idle'" : "")*/, context.getBL().getDataBaseName());

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
                DynamicExecuteEnvironment.DEFAULT, 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        Map<Integer, SQLSession> sessionMap = SQLSession.getSQLSessionMap();

        Map<String, List<Object>> resultMap = new HashMap<>();
        for (Object rsValue : rs.values()) {

            HMap entry = (HMap) rsValue;

            String query = trim((String) entry.get("query"));
            Integer processId = (Integer) entry.get("pid");
            String address = trim((String) entry.get("client_addr"));
            Timestamp dateTime = (Timestamp) entry.get("query_start");
            String state = trim((String) entry.get("state"));
            if (!query.equals(originalQuery)) {

                SQLSession sqlSession = sessionMap.get(processId);
                String userActiveTask = null;
                String computerActiveTask = null;
                if (sqlSession != null) {
                    userActiveTask = (String) findProperty("nameUser").read(context.getSession(),
                            context.getSession().getObjectValue(context.getBL().authenticationLM.user, sqlSession.userProvider.getCurrentUser()));
                    computerActiveTask = (String) findProperty("hostnameComputer").read(context.getSession(),
                            context.getSession().getObjectValue(context.getBL().authenticationLM.computer, sqlSession.userProvider.getCurrentComputer()));
                }
                resultMap.put(getSQLThreadId(sqlSession, processId), Arrays.asList(query, userActiveTask, computerActiveTask, address, dateTime,
                                state.equals("active") ? findProperty("nameStatic").readClasses(context, new DataObject("SQLUtils_StateProcess.active")) : null));
            }
        }
        return resultMap;
    }

    private String getSQLThreadId(SQLSession sqlSession, Integer processId) {
        return sqlSession != null && sqlSession.getActiveThread() != null ? String.valueOf(sqlSession.getActiveThread()) : ("s" + processId);
    }

    private Map<String, List<Object>> getActiveJavaThreads() {
        ThreadInfo[] threadsInfo = ManagementFactory.getThreadMXBean().dumpAllThreads(true, false);

        Map<String, List<Object>> resultMap = new HashMap<>();
        for(ThreadInfo threadInfo : threadsInfo) {
            int id = (int) threadInfo.getThreadId();
            Thread thread = getThreadById(id);

            String status = String.valueOf(threadInfo.getThreadState());
            String stackTrace = stackTraceToString(threadInfo.getStackTrace());
            String name = threadInfo.getThreadName();
            String lockName = threadInfo.getLockName();
            String lockOwnerId = String.valueOf(threadInfo.getLockOwnerId());
            String lockOwnerName = threadInfo.getLockOwnerName();
            LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
            String computer = logInfo == null ? null : logInfo.hostnameComputer;
            String user = logInfo == null ? null : logInfo.userName;

            resultMap.put(String.valueOf(id), Arrays.asList((Object) stackTrace, name, status, lockName, lockOwnerId, lockOwnerName, computer, user));
        }
        return resultMap;
    }

    private Thread getThreadById(int id) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    private String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        String result = sb.toString();
        return result.isEmpty() ? null : result;
    }

    private String trim(String input) {
        return input == null ? null : input.trim();
    }
}