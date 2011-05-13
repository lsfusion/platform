package skolkovo.gwt.base.shared;

public class MessageException extends Exception {
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
