package lsfusion.gwt.base.shared;

import net.customware.gwt.dispatch.shared.DispatchException;

public class MessageException extends DispatchException {
    public MessageException() {
    }
    
    public StackTraceElement[] myTrace;

    public MessageException(Throwable cause) {
        super(cause);
        myTrace = cause.getStackTrace();
    }

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
        myTrace = cause.getStackTrace();
    }
}
