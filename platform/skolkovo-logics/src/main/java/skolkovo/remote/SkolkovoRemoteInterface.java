package skolkovo.remote;

import platform.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    String[] getProjectNames(int expertId) throws RemoteException;
}
