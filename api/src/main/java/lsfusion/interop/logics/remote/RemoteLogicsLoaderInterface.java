package lsfusion.interop.logics.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsLoaderInterface extends Remote {
    RemoteLogicsInterface getLogics() throws RemoteException;
}
