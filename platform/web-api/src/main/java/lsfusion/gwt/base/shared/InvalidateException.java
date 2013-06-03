package lsfusion.gwt.base.shared;

import net.customware.gwt.dispatch.shared.DispatchException;

public class InvalidateException extends DispatchException {
    public InvalidateException() {
    }

    public InvalidateException(Throwable cause) {
        super(cause);
    }

    public InvalidateException(String message) {
        super(message);
    }

    public InvalidateException(String message, Throwable cause) {
        super(message, cause);
    }
}
