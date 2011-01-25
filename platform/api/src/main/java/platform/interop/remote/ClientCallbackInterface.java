package platform.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallbackInterface extends Remote {
    void disconnect() throws RemoteException;
    void notifyServerRestart() throws RemoteException;
}
