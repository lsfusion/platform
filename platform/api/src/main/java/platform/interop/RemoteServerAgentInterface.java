package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteServerAgentInterface extends Remote {
    void addDbName(String dbName) throws RemoteException;

    List<String> getDbNames() throws RemoteException;
}
