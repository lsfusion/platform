package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

// ошибка связи
public abstract class RemoteClientException extends RuntimeException {

    public final RemoteException cause;
    
    public RemoteClientException(RemoteException cause) {
        super(cause);
        
        this.cause = cause;        
    }
}
