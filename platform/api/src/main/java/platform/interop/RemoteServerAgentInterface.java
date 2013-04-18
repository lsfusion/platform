package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteServerAgentInterface extends Remote {
    void addExportName(String exportName) throws RemoteException;

    List<String> getExportNames() throws RemoteException;
}
