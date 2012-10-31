package platform.interop.exceptions;

public abstract class RemoteServerException extends RuntimeException {

    public RemoteServerException(String message) {
        super(message);
    }

    public RemoteServerException(Throwable cause) {
        super(cause);
    }

    public RemoteServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
