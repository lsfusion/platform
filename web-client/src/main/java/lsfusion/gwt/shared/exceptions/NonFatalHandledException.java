package lsfusion.gwt.shared.exceptions;

public class NonFatalHandledException extends Exception {
    public int count;
    public long reqId;

    public NonFatalHandledException() {
    }

    public NonFatalHandledException(String message, long reqId) {
        super(message);
        this.reqId = reqId;
    }
}
