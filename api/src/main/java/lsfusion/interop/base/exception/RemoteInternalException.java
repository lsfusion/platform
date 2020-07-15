package lsfusion.interop.base.exception;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;

// this class is needed to make throwable serializable and of specific class
public class RemoteInternalException extends RemoteServerException {
    
    public String javaStack; // for backward deserialization
    private final String lsfStack;

    public RemoteInternalException(String message, String lsfStack) {
        super(message);

        this.lsfStack = lsfStack;
    }

    // the same as in RemoteInternalDispatchException
    // returns server stacks if present, should be called outside remote calls (where aspect will wrap exceptions in remoteInternalException)
    public static Pair<String, Pair<String, String>> toString(Throwable e) {
        assert !(e instanceof RemoteMessageException); // should be handled before 
        Throwable throwable = new Throwable(ExceptionUtils.copyMessage(e));
        ExceptionUtils.copyStackTraces(e, throwable);
        return new Pair<>(throwable.getMessage(), getExStacks(throwable));
    }
    
    public static Pair<String, String> getExStacks(Throwable e) {
        return new Pair<>(getJavaStack(e), getLsfStack(e));
    }
    
    public static String getJavaStack(Throwable e) {
        return e instanceof RemoteInternalException && ((RemoteInternalException) e).javaStack != null ? ((RemoteInternalException) e).javaStack : ExceptionUtils.getStackTrace(e);
    }
    public static String getLsfStack(Throwable e) {
        return e instanceof RemoteInternalException ? ((RemoteInternalException) e).lsfStack : null;
    }
}
