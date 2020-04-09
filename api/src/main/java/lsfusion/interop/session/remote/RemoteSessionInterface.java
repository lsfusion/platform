package lsfusion.interop.session.remote;

import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;

import java.rmi.RemoteException;

public interface RemoteSessionInterface extends RemoteRequestInterface, ExecInterface {

    // main interface

    // external requests (interface is similar to RemoteLogicsInterface but without token)
    ExternalResponse exec(String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException;
}
