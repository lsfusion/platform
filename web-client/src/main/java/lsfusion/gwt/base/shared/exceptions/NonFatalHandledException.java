package lsfusion.gwt.base.shared.exceptions;

public class NonFatalHandledException extends Exception {
    public int count;
    public long reqId;

    public NonFatalHandledException() {
    }

    public NonFatalHandledException(Throwable t, long reqId) {
        super(t);
        this.reqId = reqId;
    }
}
