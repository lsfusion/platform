package lsfusion.gwt.shared.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class RemoteRetryException extends DispatchException {
    public Integer maxTries;

    public RemoteRetryException() {
    }

    public RemoteRetryException(String message, Throwable re, Integer maxTries) {
        super(message, re);
        this.maxTries = maxTries;
    }
}
