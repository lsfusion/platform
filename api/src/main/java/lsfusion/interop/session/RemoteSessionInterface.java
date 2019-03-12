package lsfusion.interop.session;

import lsfusion.interop.PendingRemoteInterface;

import java.rmi.RemoteException;

public interface RemoteSessionInterface extends PendingRemoteInterface, ExecInterface {

    // main interface

    // external requests (interface is similar to RemoteLogicsInterface but without token)
    ExternalResponse exec(String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException;

    void close() throws RemoteException;

}
