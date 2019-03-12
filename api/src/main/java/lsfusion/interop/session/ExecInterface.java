package lsfusion.interop.session;

import java.rmi.RemoteException;

public interface ExecInterface {

    // external requests
    ExternalResponse exec(String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException;    
}
