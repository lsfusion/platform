package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteServerAgentLoaderInterface extends Remote {
    void setDbName(String dbName) throws RemoteException;

    List<String> getDbNames() throws RemoteException;
}
