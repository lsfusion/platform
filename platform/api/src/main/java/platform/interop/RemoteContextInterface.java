package platform.interop;

import java.rmi.RemoteException;

public interface RemoteContextInterface {
    String getRemoteActionMessage() throws RemoteException;
}
