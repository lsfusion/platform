package lsfusion.interop.base.exception;

// message without stack (where it is obvious)
public class RemoteMessageException extends RemoteServerException {

    public RemoteMessageException(String message) {
        super(message);
    }
}
