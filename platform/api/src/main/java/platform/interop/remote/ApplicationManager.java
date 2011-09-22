package platform.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ApplicationManager extends Remote {
    public void stop() throws RemoteException;
}
