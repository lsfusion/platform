package lsfusion.gwt.base.server.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

import java.rmi.RemoteException;

public class RemoteRetryException extends DispatchException {
    public Integer maxTries;

    public RemoteRetryException(String message, RemoteException re, Integer maxTries) {
        super(message, re);
        this.maxTries = maxTries;
    }
}
