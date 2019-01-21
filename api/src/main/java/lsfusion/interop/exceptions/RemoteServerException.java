package lsfusion.interop.exceptions;

public abstract class RemoteServerException extends RuntimeException {

    public RemoteServerException(String message) {
        super(message);
    }

    public RemoteServerException(Throwable cause) {
        super(cause.getMessage()); // we can't set pass the cause further, because its class can be missing at client side
    }

    public RemoteServerException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage()); // we can't set pass the cause further, because its class can be missing at client side
    }
}
