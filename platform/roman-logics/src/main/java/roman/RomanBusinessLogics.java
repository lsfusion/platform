package roman;

import net.sf.jasperreports.engine.JRException;
import platform.interop.event.IDaemonTask;
import platform.server.auth.User;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration", "DuplicateThrows"})
public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    RomanLogicsModule RomanLM;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() {
        super.createModules();
        RomanLM = new RomanLogicsModule(LM, this);
        addLogicsModule(RomanLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
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
}
