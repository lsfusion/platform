package lsfusion.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsLoaderInterface extends Remote {
    RemoteLogicsInterface getLogics() throws RemoteException;

    byte[] findClass(String name) throws RemoteException;
}
