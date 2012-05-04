package platform.gwt.paas.server.exceptions;

import com.gwtplatform.dispatch.shared.ActionException;

public class IOActionException extends ActionException {
    public IOActionException() {
    }

    public IOActionException(Throwable cause) {
        super(cause);
    }

    public IOActionException(String message) {
        super(message);
    }

    public IOActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
