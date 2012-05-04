package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteContextInterface extends Remote {
    String getRemoteActionMessage() throws RemoteException;
}
