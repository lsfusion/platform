package platform.agent;

import platform.interop.RemoteServerAgentInterface;
import platform.interop.remote.RemoteObject;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ServerAgent extends RemoteObject implements RemoteServerAgentInterface {
    private final List<String> dbNames = new ArrayList<String>();

    public ServerAgent(final int exportPort) throws RemoteException {
        super(exportPort, true);
    }

    public void addDbName(String dbName) {
        if (!dbNames.contains(dbName)) {
            dbNames.add(dbName);
        }
    }

    public List<String> getDbNames() {
        return dbNames;
    }
}