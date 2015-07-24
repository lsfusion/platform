package lsfusion.gwt.base.server.exceptions;

import java.rmi.RemoteException;

public class RemoteFormDispatchException extends RemoteDispatchException {
    public RemoteFormDispatchException(String message, RemoteException cause) {
        super(message, cause);
    }
}
