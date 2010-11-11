package platform.interop.exceptions;

public abstract class RemoteServerException extends RuntimeException {

    public RemoteServerException(String message) {
        super(message);
    }
}
