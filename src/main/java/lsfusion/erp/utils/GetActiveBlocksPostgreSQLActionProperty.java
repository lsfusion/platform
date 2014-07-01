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

public class GetActiveBlocksPostgreSQLActionProperty extends ScriptingActionProperty {

    public GetActiveBlocksPostgreSQLActionProperty(ScriptingLogicsModule LM) {
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

        Integer previousCount = (Integer) getLCP("previousCountActiveBlock").read(session);
        previousCount = previousCount == null ? 0 : previousCount;

        for (int i = 0; i < previousCount; i++) {
            DataObject currentObject = new DataObject(i);
            getLCP("typeLockedObjectActiveBlock").change((Object) null, session, currentObject);
            getLCP("idLockedObjectActiveBlock").change((Object) null, session, currentObject);
            getLCP("processIdActiveBlock").change((Object) null, session, currentObject);
            getLCP("modeActiveBlock").change((Object) null, session, currentObject);
            getLCP("grantedActiveBlock").change((Object) null, session, currentObject);
        }

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
                getLCP("idLockedObjectActiveBlock").change(transactionID, session, currentObject);
            else if (type != null && type.equals("virtualxid"))
                getLCP("idLockedObjectActiveBlock").change(virtualxID, session, currentObject);
            else if (type != null && type.equals("relation"))
                getLCP("idLockedObjectActiveBlock").change(relationID, session, currentObject);

            getLCP("processIdActiveBlock").change(processId, session, currentObject);
            getLCP("modeActiveBlock").change(mode, session, currentObject);
            getLCP("typeLockedObjectActiveBlock").change(lockedType, session, currentObject);
            getLCP("grantedActiveBlock").change(granted, session, currentObject);
            
            i++;
        }
        getLCP("previousCountActiveBlock").change(i, session);
    }

    protected String trim(String input) {
        return input == null ? null : input.trim();
    }
}