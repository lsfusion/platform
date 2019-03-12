package lsfusion.interop.exception;

public abstract class RemoteServerException extends RuntimeException {

    public RemoteServerException(String message) {
        super(message);
    }
}
