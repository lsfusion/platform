package platform.interop.remote;

import java.rmi.RemoteException;

public interface PingRemote {
    void ping() throws RemoteException;
}
