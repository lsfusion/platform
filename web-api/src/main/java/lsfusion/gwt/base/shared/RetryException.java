package lsfusion.gwt.base.shared;


import net.customware.gwt.dispatch.shared.DispatchException;

public class RetryException extends DispatchException {
    public Integer maxTries;

    public RetryException(String message, Integer maxTries) {
        super(message);
        this.maxTries = maxTries;
    }

    public RetryException() {
    }
}
