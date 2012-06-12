package roman;

import net.sf.jasperreports.engine.JRException;
import platform.interop.event.IDaemonTask;
import platform.server.auth.SecurityPolicy;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration", "DuplicateThrows"})
public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    public ScriptingLogicsModule Stock;
    public RomanLogicsModule RomanLM;
    public ScriptingLogicsModule RomanRB;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);

        this.setDialogUndecorated(false);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        Stock = addModuleFromResource("/scripts/Stock.lsf");
        addModulesFromResource(
            "/scripts/Utils.lsf",
            "/scripts/Hierarchy.lsf",
            "/scripts/Historizable.lsf",
            "/scripts/Numerator.lsf",
            "/scripts/Document.lsf",
            "/scripts/Consignment.lsf",
            "/scripts/LegalEntity.lsf",
            "/scripts/Employee.lsf",
            "/scripts/Tax.lsf",
            "/scripts/Ware.lsf",
            "/scripts/Declaration.lsf"
        );
        RomanLM = addModule(new RomanLogicsModule(LM, this));
        RomanLM.setRequiredModules(Arrays.asList("System", "Utils", "Hierarchy", "Historizable", "Numerator", "Stock", "Document"));

        RomanRB = addModuleFromResource("/scripts/RomanRB.lsf");
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }


    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        ArrayList<IDaemonTask> daemons = super.getDaemonTasks(compId);

        Integer scalesComPort, scalesSpeed, scannerComPort;
        try {
            DataSession session = createSession();
            scalesComPort = (Integer) RomanLM.scalesComPort.read(session, new DataObject(compId, LM.computer));
            scalesSpeed = (Integer) RomanLM.scalesSpeed.read(session, new DataObject(compId, LM.computer));
            scannerComPort = (Integer) RomanLM.scannerComPort.read(session, new DataObject(compId, LM.computer));
            session.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (scalesComPort != null) {
            IDaemonTask task = new WeightDaemonTask(scalesComPort, scalesSpeed, 1000, 0);
            daemons.add(task);
        }
        if (scannerComPort != null) {
            IDaemonTask task = new ScannerDaemonTask(scannerComPort);
            daemons.add(task);
        }
        return daemons;
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }
}
