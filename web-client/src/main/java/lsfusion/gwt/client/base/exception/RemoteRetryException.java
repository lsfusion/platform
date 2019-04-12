package lsfusion.gwt.client.base.exception;

import net.customware.gwt.dispatch.shared.DispatchException;

public class RemoteRetryException extends DispatchException {
    public Integer maxTries;

    public RemoteRetryException() {
    }

    public RemoteRetryException(Throwable re, Integer maxTries) {
        super(re);
        this.maxTries = maxTries;
    }
}
