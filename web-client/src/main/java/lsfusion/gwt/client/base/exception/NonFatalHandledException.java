package lsfusion.gwt.client.base.exception;

import com.google.gwt.core.shared.SerializableThrowable;

public class NonFatalHandledException extends Exception {
    public int count;
    public long reqId;

    public SerializableThrowable thisStack; // needed to serialize stack

    public NonFatalHandledException() {
    }

    public NonFatalHandledException(String message, SerializableThrowable thisStack, long reqId) {
        super(message);
        this.thisStack = thisStack;
        this.reqId = reqId;
    }
}
