package platform.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallBackInterface extends Remote {
    void denyRestart() throws RemoteException;
    List<CallbackMessage> pullMessages() throws RemoteException;
}
