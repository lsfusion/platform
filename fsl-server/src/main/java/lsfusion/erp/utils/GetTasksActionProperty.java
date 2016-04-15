package lsfusion.erp.utils;

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
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

public abstract class GetTasksActionProperty extends ScriptingActionProperty {

    public GetTasksActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    protected void getTasksFromDatabase(ExecutionContext context, boolean onlyActive) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        DataSession session = context.getSession();

        Integer previousCount = (Integer) findProperty("previousCountActiveTask[]").read(session);
        previousCount = previousCount == null ? 0 : previousCount;

        for (int i = 0; i < previousCount; i++) {
            DataObject currentObject = new DataObject(i);
            findProperty("idActiveTask[INTEGER]").change((Object) null, session, currentObject);
            findProperty("queryActiveTask[INTEGER]").change((Object) null, session, currentObject);
            findProperty("userActiveTask[INTEGER]").change((Object) null, session, currentObject);
            findProperty("addressUserActiveTask[INTEGER]").change((Object) null, session, currentObject);
            findProperty("dateTimeActiveTask[INTEGER]").change((Object) null, session, currentObject);
        }

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

        if (syntaxType == SQLSyntaxType.MSSQL) {
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

            ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                    0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            int i = 0;
            for (Object rsValue : rs.values()) {

                HMap entry = (HMap) rsValue;

                DataObject currentObject = new DataObject(i);

                String query = trim((String) entry.get("text"));
                Integer processId = (Integer) entry.get("session_id");
                String userActiveTask = trim((String) entry.get("host_name"));
                String address = trim((String) entry.get("client_net_address"));
                Timestamp dateTime = (Timestamp) entry.get("start_time");
                if (!query.equals(originalQuery)) {
                    findProperty("idActiveTask[INTEGER]").change(processId, session, currentObject);
                    findProperty("queryActiveTask[INTEGER]").change(query, session, currentObject);
                    findProperty("userActiveTask[INTEGER]").change(userActiveTask, session, currentObject);
                    findProperty("addressUserActiveTask[INTEGER]").change(address, session, currentObject);
                    findProperty("dateTimeActiveTask[INTEGER]").change(dateTime, session, currentObject);
                    i++;
                }
            }
            findProperty("previousCountActiveTask[]").change(i, session);

        } else if (syntaxType == SQLSyntaxType.POSTGRES) {

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
            propertyNames.immutable();

            MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
            propertyReaders.exclAdd("query", StringClass.get(1000));
            propertyReaders.exclAdd("pid", IntegerClass.instance);
            propertyReaders.exclAdd("usename", StringClass.get(100));
            propertyReaders.exclAdd("client_addr", PGObjectReader.instance);
            propertyReaders.exclAdd("query_start", DateTimeClass.instance);
            propertyReaders.immutable();

            ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                    0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            Map<Integer, SQLSession> sessionMap = SQLSession.getSQLSessionMap();
            
            int i = 0;
            for (Object rsValue : rs.values()) {

                HMap entry = (HMap) rsValue;

                DataObject currentObject = new DataObject(i);

                String query = trim((String) entry.get("query"));
                Integer processId = (Integer) entry.get("pid");
                //String userActiveTask = trim((String) entry.get("usename"));
                String address = trim((String) entry.get("client_addr"));
                Timestamp dateTime = (Timestamp) entry.get("query_start");
                if (!query.equals(originalQuery)) {

                    SQLSession sqlSession = sessionMap.get(processId);
                    String userActiveTask = null;
                    String computerActiveTask = null;
                    if (sqlSession != null) {
                        userActiveTask = (String) findProperty("name[User]").read(session,
                                session.getObjectValue(context.getBL().authenticationLM.user, sqlSession.userProvider.getCurrentUser()));
                        computerActiveTask = (String) findProperty("hostname[Computer]").read(session,
                                session.getObjectValue(context.getBL().authenticationLM.computer, sqlSession.userProvider.getCurrentComputer()));
                    }
                    findProperty("idActiveTask[INTEGER]").change(processId, session, currentObject);
                    findProperty("queryActiveTask[INTEGER]").change(query, session, currentObject);
                    findProperty("computerActiveTask[INTEGER]").change(computerActiveTask, session, currentObject);
                    findProperty("userActiveTask[INTEGER]").change(userActiveTask, session, currentObject);
                    findProperty("addressUserActiveTask[INTEGER]").change(address, session, currentObject);
                    findProperty("dateTimeActiveTask[INTEGER]").change(dateTime, session, currentObject);
                    i++;
                }
            }
            findProperty("previousCountActiveTask[]").change(i, session);
        }
    }

    private String trim(String input) {
        return input == null ? null : input.trim();
    }
}