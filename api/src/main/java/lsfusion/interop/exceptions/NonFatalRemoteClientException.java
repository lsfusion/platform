package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

public class NonFatalRemoteClientException extends RemoteClientException {

    public int count;
    public boolean abandoned;
    
    public NonFatalRemoteClientException(String message, long reqId) {
        super(message, reqId);
    }

    public NonFatalRemoteClientException(String message, int count, long reqId) {
        super(message, reqId);
        this.count = count;
    }
}
