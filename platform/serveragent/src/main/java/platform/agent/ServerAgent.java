package platform.agent;

import platform.interop.RemoteServerAgentInterface;
import platform.interop.remote.RemoteObject;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ServerAgent extends RemoteObject implements RemoteServerAgentInterface {
    private final List<String> exportNames = new ArrayList<String>();

    public ServerAgent(final int exportPort) throws RemoteException {
        super(exportPort, true);
    }

    public void addExportName(String exportName) {
        if (!exportNames.contains(exportName)) {
            exportNames.add(exportName);
        }
    }

    public List<String> getExportNames() {
        return exportNames;
    }
}