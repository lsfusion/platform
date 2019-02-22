package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

// ошибка связи
public abstract class RemoteClientException extends RuntimeException {

    public RemoteClientException(RemoteException cause) {
        super(cause);
    }

    public RemoteClientException(String message) {
        super(message);
    }
}
