package lsfusion.gwt.base.server.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class IODispatchException extends DispatchException {
    public IODispatchException() {
    }

    public IODispatchException(Throwable cause) {
        super(cause);
    }

    public IODispatchException(String message) {
        super(message);
    }

    public IODispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
