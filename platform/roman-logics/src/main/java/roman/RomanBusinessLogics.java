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
    public ScriptingLogicsModule Utils;
    public ScriptingLogicsModule Hierarchy;
    public ScriptingLogicsModule Historizable;
    public ScriptingLogicsModule Numerator;
    public ScriptingLogicsModule Stock;
    public ScriptingLogicsModule Document;
    public ScriptingLogicsModule Consignment;
    public ScriptingLogicsModule LegalEntity;
    public ScriptingLogicsModule Employee;
    public ScriptingLogicsModule Ware;
    public ScriptingLogicsModule Tax;

    public RomanLogicsModule RomanLM;
    public ScriptingLogicsModule RomanRB;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);

        this.setDialogUndecorated(false);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        Utils = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Utils.lsf"), LM, this));
        Hierarchy = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Hierarchy.lsf"), LM, this));
        Historizable = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Historizable.lsf"), LM, this));
        Numerator = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Numerator.lsf"), LM, this));
        Stock = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Stock.lsf"), LM, this));
        Document = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Document.lsf"), LM, this));
        Consignment = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Consignment.lsf"), LM, this));
        LegalEntity = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/LegalEntity.lsf"), LM, this));
        Employee = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Employee.lsf"), LM, this));
        Ware = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Tax.lsf"), LM, this));
        Tax = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/Ware.lsf"), LM, this));
        RomanLM = addLogicsModule(new RomanLogicsModule(LM, this));
        RomanLM.setRequiredModules(Arrays.asList("System", "Utils", "Hierarchy", "Historizable", "Numerator", "Stock", "Document"));
        RomanRB = addLogicsModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/RomanRB.lsf"), LM, this));
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
