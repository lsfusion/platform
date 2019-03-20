package lsfusion.interop.logics;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsLoaderInterface extends Remote {
    RemoteLogicsInterface getLogics() throws RemoteException;
}
