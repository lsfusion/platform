package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

public class FatalRemoteClientException extends RemoteClientException {
    
    public FatalRemoteClientException(String message, long reqId) {
        super(message, reqId);
    }
}
