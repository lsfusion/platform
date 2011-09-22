package platform.gwt.paas.shared.exceptions;

import com.gwtplatform.dispatch.shared.ActionException;

public class MessageException extends ActionException {
    public MessageException() {
    }

    public MessageException(Throwable cause) {
        super(cause);
    }

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
