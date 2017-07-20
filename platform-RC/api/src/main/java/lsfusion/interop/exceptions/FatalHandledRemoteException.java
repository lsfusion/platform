package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

public class FatalHandledRemoteException extends HandledRemoteException {
    
    public FatalHandledRemoteException(RemoteException cause, long reqId) {
        super(cause, reqId);
    }
}
