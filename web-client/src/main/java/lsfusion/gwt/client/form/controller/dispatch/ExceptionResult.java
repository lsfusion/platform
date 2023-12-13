package lsfusion.gwt.client.form.controller.dispatch;

public class ExceptionResult {
    public long requestIndex;
    public Throwable throwable;

    public ExceptionResult(long requestIndex, Throwable throwable) {
        this.requestIndex = requestIndex;
        this.throwable = throwable;
    }
}