package lsfusion.server.physics.admin.monitor.action;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import org.postgresql.replication.LogSequenceNumber;

import java.sql.SQLException;

public class UpdateServersAction extends ProcessDumpAction {
    public UpdateServersAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataAdapter dataAdapter = context.getDbManager().getAdapter();
        ServiceLogicsModule serviceLM = context.getBL().serviceLM;
        DataSession session = context.getSession();
        try {
            for (ImList<DataObject> server : serviceLM.is(serviceLM.dbServer).readAllClasses(session).keys()) {
                DataObject serverDataObject = server.get(0);
                DataAdapter.Server dbServer = serverDataObject.objectClass.equals(serviceLM.dbSlave) ?
                        dataAdapter.findSlave(serverDataObject.object.toString()) : dataAdapter.getMaster();

                if (dbServer != null) {
                    serviceLM.findProperty("load[DBServer]").change(dbServer.getLoad() * 100, session, serverDataObject);

                    LogSequenceNumber lsn = dbServer.isMaster() ? dataAdapter.getMasterLSN() : dataAdapter.getSlaveLSN(dbServer);
                    serviceLM.findProperty("lsn[DBServer]").change(lsn.toString(), session, serverDataObject);

                    boolean readyStatus = dbServer.isMaster() || dataAdapter.readSlaveReady((DataAdapter.Slave) dbServer);
                    serviceLM.findProperty("readyStatus[DBServer]").change(readyStatus, session, serverDataObject);

                    boolean availability = dataAdapter.isServerAvailable(dbServer);
                    serviceLM.findProperty("availability[DBServer]").change(availability, session, serverDataObject);

                    double lag = dbServer.isMaster() ? 0.0 : dataAdapter.readSlaveLag((DataAdapter.Slave) dbServer);
                    serviceLM.findProperty("lag[DBServer]").change(lag, session, serverDataObject);

                    double usedCpu = dbServer.usedCpu;
                    serviceLM.findProperty("usedCpu[DBServer]").change(usedCpu * 100, session, serverDataObject);

                    int numberConnections = dataAdapter.getNumberOfConnections(dbServer);
                    serviceLM.findProperty("numberConnections[DBServer]").change(numberConnections, session, serverDataObject);
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}
