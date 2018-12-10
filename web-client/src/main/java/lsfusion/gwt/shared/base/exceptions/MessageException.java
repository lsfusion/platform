package lsfusion.gwt.shared.base.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class MessageException extends DispatchException {
    public MessageException() {
    }
    
    public StackTraceElement[] myTrace;
    public String lsfStack;

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
    
    public MessageException(String message, Throwable cause, String lsfStack) {
        this(message, cause);
        this.lsfStack = lsfStack;
    }
}
