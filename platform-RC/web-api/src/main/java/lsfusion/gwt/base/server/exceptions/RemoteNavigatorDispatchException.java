package lsfusion.gwt.base.server.exceptions;

import java.rmi.RemoteException;

public class RemoteNavigatorDispatchException extends RemoteDispatchException {
    public RemoteNavigatorDispatchException(String message, RemoteException cause) {
        super(message, cause);
    }
}
