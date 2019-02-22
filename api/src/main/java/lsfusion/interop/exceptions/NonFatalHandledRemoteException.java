package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

public class NonFatalHandledRemoteException extends HandledRemoteException {

    public int count;
    public boolean abandoned;
    
    public NonFatalHandledRemoteException(RemoteException cause, long reqId) {
        super(cause, reqId);
    }

    public NonFatalHandledRemoteException(String message, int count, long reqId) {
        super(message, reqId);
        this.count = count;
    }
}
