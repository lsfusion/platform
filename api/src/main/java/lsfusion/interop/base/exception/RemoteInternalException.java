package lsfusion.interop.base.exception;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;

// this class is needed to make throwable serializable and of specific class
public class RemoteInternalException extends RemoteServerException {
    
    public String javaStack; // for backward deserialization

    private final String lsfStack;
    private final String asyncStacks; // java + lsf
    public final Integer status;

    public RemoteInternalException(String message, String lsfStack, String asyncStacks) {
        this(message, lsfStack, asyncStacks, null);
    }

    public RemoteInternalException(String message, String lsfStack, String asyncStacks, Integer status) {
        super(message);

        this.lsfStack = lsfStack;
        this.asyncStacks = asyncStacks;
        this.status = status;
    }

    // the same as in RemoteInternalDispatchException
    // returns server stacks if present, should be called outside remote calls (where aspect will wrap exceptions in remoteInternalException)
    public static Pair<String, ExStacks> toString(Throwable e) {
        assert !(e instanceof RemoteMessageException); // should be handled before 
        Throwable throwable = new Throwable(ExceptionUtils.copyMessage(e));
        ExceptionUtils.copyStackTraces(e, throwable);
        return new Pair<>(throwable.getMessage(), getExStacks(throwable));
    }

    public static ExStacks getExStacks(Throwable e) {
        return new ExStacks(getJavaStack(e), getLsfStack(e), getAsyncStacks(e));
    }
    
    public static String getLsfStack(Throwable e) {
        return e instanceof RemoteInternalException ? ((RemoteInternalException) e).lsfStack : "";
    }
    
    public static String getJavaStack(Throwable e) {
        return e instanceof RemoteInternalException && ((RemoteInternalException) e).javaStack != null ? ((RemoteInternalException) e).javaStack : ExceptionUtils.getStackTrace(e);
    }

    public static String getAsyncStacks(Throwable e) {
        return e instanceof RemoteInternalException ? ((RemoteInternalException) e).asyncStacks : "";
    }

    public static class ExStacks {
        public final String javaStack;
        public final String lsfStack;
        public final String asyncStacks;

        public ExStacks(String javaStack, String lsfStack, String asyncStacks) {
            this.javaStack = javaStack;
            this.lsfStack = lsfStack;
            this.asyncStacks = asyncStacks;
        }
    }
}
