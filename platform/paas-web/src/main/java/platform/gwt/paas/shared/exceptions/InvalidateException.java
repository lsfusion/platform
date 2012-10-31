package platform.gwt.paas.shared.exceptions;

public class InvalidateException extends MessageException {
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
