package lsfusion.server.remote;

import lsfusion.server.stack.ThrowableWithStack;

public class InvocationResult {
    public static final InvocationResult PAUSED = new InvocationResult(Status.PAUSED);
    public static final InvocationResult FINISHED = new InvocationResult(Status.FINISHED);

    public enum Status {
        PAUSED, EXCEPTION_THROWN, FINISHED
    }

    private final Status status;
    private final ThrowableWithStack throwable;

    public InvocationResult(Throwable t) {
        this(Status.EXCEPTION_THROWN, new ThrowableWithStack(t));
    }

    public InvocationResult(Status status) {
        this(status, null);
    }

    private InvocationResult(Status status, ThrowableWithStack throwable) {
        this.status = status;
        this.throwable = throwable;
    }

    public Status getStatus() {
        return status;
    }

    public ThrowableWithStack getThrowable() {
        return throwable;
    }
}
