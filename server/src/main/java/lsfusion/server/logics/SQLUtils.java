package lsfusion.server.logics;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.trimToNull;

public class SQLUtils {

    public static void killSQLProcess(BusinessLogics BL, DBManager dbManager, long id) throws SQLException, SQLHandledException {
        Map<Integer, Integer> sqlProcesses = dbManager.getAdapter().getSyntaxType() == SQLSyntaxType.POSTGRES ? getPostgresProcesses(BL, dbManager) : getMSSQLProcesses(dbManager);
        Integer sqlProcess = sqlProcesses.get((int) id);
        if(sqlProcess != null)
            dbManager.getAdapter().killProcess(sqlProcess);
    }

    private static Map<Integer, Integer> getMSSQLProcesses(DBManager dbManager) throws SQLException, SQLHandledException {
        Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();
        String originalQuery = "Select A.session_id, text\n" +
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
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("text", StringClass.get(1000));
        propertyReaders.exclAdd("session_id", IntegerClass.instance);
        propertyReaders.immutable();

        try(DataSession session = dbManager.createSession()) {
            ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap()
                    , 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            Map<Integer, Integer> resultMap = new HashMap<>();
            for (Object rsValue : rs.values()) {
                HMap entry = (HMap) rsValue;
                String query = trimToNull((String) entry.get("text"));
                Integer processId = (Integer) entry.get("session_id");

                if (!query.equals(originalQuery)) {
                    List<Object> sessionThread = sessionThreadMap.get(processId);
                    if (sessionThread != null && sessionThread.get(0) != null) {
                        resultMap.put((Integer) sessionThread.get(0), processId);
                    }
                }
            }
            return resultMap;
        }
    }

    private static Map<Integer, Integer> getPostgresProcesses(BusinessLogics BL, DBManager dbManager) throws SQLException, SQLHandledException {

        String originalQuery = String.format("SELECT * FROM pg_stat_activity WHERE datname='%s'", BL.getDataBaseName());

        MExclSet<String> keyNames = SetFact.mExclSet();
        keyNames.exclAdd("numberrow");
        keyNames.immutable();

        MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
        keyReaders.exclAdd("numberrow", new CustomReader());
        keyReaders.immutable();

        MExclSet<String> propertyNames = SetFact.mExclSet();
        propertyNames.exclAdd("query");
        propertyNames.exclAdd("pid");
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("query", StringClass.get(1000));
        propertyReaders.exclAdd("pid", IntegerClass.instance);
        propertyReaders.immutable();

        try(DataSession session = dbManager.createSession()) {
            ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                    0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            Map<Integer, List<Object>> sessionThreadMap = SQLSession.getSQLThreadMap();

            Map<Integer, Integer> resultMap = new HashMap<>();
            for (Object rsValue : rs.values()) {
                HMap entry = (HMap) rsValue;
                String query = trimToNull((String) entry.get("query"));
                Integer processId = (Integer) entry.get("pid");
                if (!query.equals(originalQuery)) {
                    List<Object> sessionThread = sessionThreadMap.get(processId);
                    if(sessionThread != null && sessionThread.get(0) != null)
                        resultMap.put((Integer) sessionThread.get(0), processId);
                }
            }
            return resultMap;
        }
    }
}
