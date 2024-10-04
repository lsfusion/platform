package lsfusion.interop.connection;

import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;

import java.rmi.RemoteException;

public interface RemoteConnectionInterface extends RemoteRequestInterface {

    // external requests (interface is similar to RemoteLogicsInterface but without token)
    ExternalResponse exec(String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException;
}
