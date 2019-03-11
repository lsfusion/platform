package lsfusion.interop.exceptions;

import java.rmi.RemoteException;

// ошибка связи
public abstract class RemoteClientException extends RuntimeException {

    public final long reqId;

    public RemoteClientException(String message, long reqId) {
        super(message);

        this.reqId = reqId;
    }
}
