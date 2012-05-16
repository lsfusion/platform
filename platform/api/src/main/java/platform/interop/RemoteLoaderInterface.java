package platform.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteLoaderInterface extends Remote {
    RemoteLogicsInterface getRemoteLogics() throws RemoteException;

    byte[] findClass(String name) throws RemoteException;
    
    void setDbName(String dbName) throws RemoteException;

    List<String> getDbNames() throws RemoteException;
}
