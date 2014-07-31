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
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.QueryExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Reader;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class GetActiveBlocksActionProperty extends ScriptingActionProperty {

    public GetActiveBlocksActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            getActiveBlocksFromDatabase(context);

        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }


    private void getActiveBlocksFromDatabase(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        DataSession session = context.getSession();

        Integer previousCount = (Integer) findProperty("previousCountActiveBlock").read(session);
        previousCount = previousCount == null ? 0 : previousCount;

        for (int i = 0; i < previousCount; i++) {
            DataObject currentObject = new DataObject(i);
            findProperty("typeLockedObjectActiveBlock").change((Object) null, session, currentObject);
            findProperty("idLockedObjectActiveBlock").change((Object) null, session, currentObject);
            findProperty("processIdActiveBlock").change((Object) null, session, currentObject);
            findProperty("modeActiveBlock").change((Object) null, session, currentObject);
            findProperty("grantedActiveBlock").change((Object) null, session, currentObject);
        }

        SQLSyntaxType syntaxType = context.getDbManager().getAdapter().getSyntaxType();

        if (syntaxType == SQLSyntaxType.MSSQL) {
            String originalQuery = "select convert(varchar(30), suser_sname(p.sid)) as login, \n" +
                    "       convert (smallint, req_spid) As spid, \n" +
                    "       convert(varchar(30), db_name(rsc_dbid)) As db_name, \n" +
                    "       case rsc_dbid when db_id() \n" +
                    "            then convert(varchar(30), object_name(rsc_objid)) \n" +
                    "            else convert(varchar(30), rsc_objid) end As Object, \n" +
                    "       rsc_indid As indid, \n" +
                    "       substring (lock_type.name, 1, 4) As Type, \n" +
                    "       substring (lock_mode.name, 1, 12) As Mode, \n" +
                    "       lock_status.name As Status, \n" +
                    "       substring (rsc_text, 1, 16) as Resource, \n" +
                    "    (SELECT text FROM ::fn_get_sql(p.sql_handle) t)\n" +
                    "   from master..syslockinfo s \n" +
                    "   join master..spt_values lock_type on s.rsc_type = lock_type.number \n" +
                    "   join master..spt_values lock_status on s.req_status = lock_status.number \n" +
                    "   join master..spt_values lock_mode on s.req_mode = lock_mode.number -1 \n" +
                    "   join master..sysprocesses p on s.req_spid = p.spid    \n" +
                    "   where  \n" +
                    "     lock_type.type = 'LR' \n" +
                    "     and lock_status.type = 'LS' \n" +
                    "     and lock_mode.type = 'L' \n" +
                    "     and db_name(rsc_dbid) not in ('master', 'msdb', 'tempdb', 'model')  \n" +
                    "  --AND lock_status.name = 'WAIT'\n" +
                    "order by resource, status, Object, type, spid, lock_type.number";

            MExclSet<String> keyNames = SetFact.mExclSet();
            keyNames.exclAdd("numberrow");
            keyNames.immutable();

            MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
            keyReaders.exclAdd("numberrow", new CustomReader());
            keyReaders.immutable();

            MExclSet<String> propertyNames = SetFact.mExclSet();
            propertyNames.exclAdd("Mode");
            propertyNames.exclAdd("Type");
            propertyNames.exclAdd("spid");
            propertyNames.exclAdd("Object");
            propertyNames.exclAdd("Status");
            propertyNames.immutable();

            MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
            propertyReaders.exclAdd("Mode", StringClass.get(100));
            propertyReaders.exclAdd("Type", StringClass.get(100));
            propertyReaders.exclAdd("spid", IntegerClass.instance);
            propertyReaders.exclAdd("Object", StringClass.get(100));
            propertyReaders.exclAdd("Status", StringClass.get(100));
            propertyReaders.immutable();

             ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, ExecuteEnvironment.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                    QueryExecuteEnvironment.DEFAULT, 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            int i = 0;
            for (Object rsValue : rs.values()) {

                HMap entry = (HMap) rsValue;


                DataObject currentObject = new DataObject(i);

                String mode = trim((String) entry.get("Mode"));
                String lockedType = trim((String) entry.get("Type"));
                Integer processId = (Integer) entry.get("spid");
                String objectId = trim((String) entry.get("Object"));
                Boolean granted = trim((String) entry.get("Status")).equals("GRANT") ? true : null;

                findProperty("idLockedObjectActiveBlock").change(objectId, session, currentObject);
                findProperty("processIdActiveBlock").change(processId, session, currentObject);
                findProperty("modeActiveBlock").change(mode, session, currentObject);
                findProperty("typeLockedObjectActiveBlock").change(lockedType, session, currentObject);
                findProperty("grantedActiveBlock").change(granted, session, currentObject);

                i++;
            }
            findProperty("previousCountActiveBlock").change(i, session);
                      
            
        }  else if (syntaxType == SQLSyntaxType.POSTGRES) {
            String originalQuery = "SELECT relation::regclass, * FROM pg_locks";

            MExclSet<String> keyNames = SetFact.mExclSet();
            keyNames.exclAdd("numberrow");
            keyNames.immutable();

            MExclMap<String, Reader> keyReaders = MapFact.mExclMap();
            keyReaders.exclAdd("numberrow", new CustomReader());
            keyReaders.immutable();

            MExclSet<String> propertyNames = SetFact.mExclSet();
            propertyNames.exclAdd("transactionid");
            propertyNames.exclAdd("virtualxid");
            propertyNames.exclAdd("relation");
            propertyNames.exclAdd("locktype");
            propertyNames.exclAdd("pid");
            propertyNames.exclAdd("mode");
            propertyNames.exclAdd("granted");
            propertyNames.immutable();

            MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
            propertyReaders.exclAdd("transactionid", PGObjectReader.instance);
            propertyReaders.exclAdd("virtualxid", StringClass.get(100));
            propertyReaders.exclAdd("relation", PGObjectReader.instance);
            propertyReaders.exclAdd("locktype", StringClass.get(100));
            propertyReaders.exclAdd("pid", IntegerClass.instance);
            propertyReaders.exclAdd("mode", StringClass.get(100));
            propertyReaders.exclAdd("granted", LogicalClass.instance);
            propertyReaders.immutable();

            ImOrderMap rs = session.sql.executeSelect(originalQuery, OperationOwner.unknown, ExecuteEnvironment.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                    QueryExecuteEnvironment.DEFAULT, 0, ((ImSet) keyNames).toRevMap(), (ImMap) keyReaders, ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

            int i = 0;
            for (Object rsValue : rs.values()) {

                HMap entry = (HMap) rsValue;


                DataObject currentObject = new DataObject(i);

                String transactionID = trim((String) entry.get("transactionid"));
                String virtualxID = trim((String) entry.get("virtualxid"));
                String relationID = trim((String) entry.get("relation"));
                String type = trim((String) entry.get("locktype"));
                String mode = trim((String) entry.get("mode"));
                String lockedType = trim((String) entry.get("locktype"));
                Integer processId = (Integer) entry.get("pid");
                Boolean granted = (Boolean) entry.get("granted");

                if (type != null && type.equals("transactionid"))
                    findProperty("idLockedObjectActiveBlock").change(transactionID, session, currentObject);
                else if (type != null && type.equals("virtualxid"))
                    findProperty("idLockedObjectActiveBlock").change(virtualxID, session, currentObject);
                else if (type != null && type.equals("relation"))
                    findProperty("idLockedObjectActiveBlock").change(relationID, session, currentObject);

                findProperty("processIdActiveBlock").change(processId, session, currentObject);
                findProperty("modeActiveBlock").change(mode, session, currentObject);
                findProperty("typeLockedObjectActiveBlock").change(lockedType, session, currentObject);
                findProperty("grantedActiveBlock").change(granted, session, currentObject);

                i++;
            }
            findProperty("previousCountActiveBlock").change(i, session);
        }
    }

    protected String trim(String input) {
        return input == null ? null : input.trim();
    }
}