package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

public abstract class HandledRemoteException extends RemoteClientException {

    public final long reqId;
    
    public HandledRemoteException(RemoteException cause, long reqId) {
        super(cause);
        
        this.reqId = reqId;
    }
}
