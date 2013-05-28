package platform.gwt.base.server.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

import java.rmi.RemoteException;

public class RemoteDispatchException extends DispatchException {
    private final RemoteException remote;

    public RemoteDispatchException(String message, RemoteException cause) {
        super(message, cause);
        remote = cause;
    }

    public RemoteException getRemote() {
        return remote;
    }
}
