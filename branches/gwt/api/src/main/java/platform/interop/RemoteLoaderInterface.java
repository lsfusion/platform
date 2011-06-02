package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLoaderInterface extends Remote {
    RemoteLogicsInterface getRemoteLogics() throws RemoteException;

    byte[] findClass(String name) throws RemoteException;
}
