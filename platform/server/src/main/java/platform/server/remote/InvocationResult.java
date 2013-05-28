package platform.server.remote;

public class InvocationResult {
    public static final InvocationResult PAUSED = new InvocationResult(Status.PAUSED);
    public static final InvocationResult FINISHED = new InvocationResult(Status.FINISHED);

    public static enum Status {
        PAUSED, EXCEPTION_THROWN, FINISHED
    }

    private final Status status;
    private final Throwable throwable;

    public InvocationResult(Throwable t) {
        this(Status.EXCEPTION_THROWN, t);
    }

    public InvocationResult(Status status) {
        this(status, null);
    }

    private InvocationResult(Status status, Throwable throwable) {
        this.status = status;
        this.throwable = throwable;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
