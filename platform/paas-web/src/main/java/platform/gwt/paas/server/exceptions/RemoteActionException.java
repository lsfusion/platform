package platform.gwt.paas.server.exceptions;

import com.gwtplatform.dispatch.shared.ActionException;

import java.rmi.RemoteException;

public class RemoteActionException extends ActionException {
    private final RemoteException remote;

    public RemoteActionException(String message, RemoteException cause) {
        super(message, cause);
        remote = cause;
    }

    public RemoteException getRemote() {
        return remote;
    }
}
