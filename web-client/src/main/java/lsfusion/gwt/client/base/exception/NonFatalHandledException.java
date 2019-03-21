package lsfusion.gwt.client.base.exception;

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
