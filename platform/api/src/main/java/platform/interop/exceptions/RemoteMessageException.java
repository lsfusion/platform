package platform.interop.exceptions;

public class RemoteMessageException extends RemoteServerException {

    public RemoteMessageException(String message) {
        super(message);
    }

    public RemoteMessageException(Throwable cause) {
        super(cause);
    }

    public RemoteMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
