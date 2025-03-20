package lsfusion.gwt.client.base.exception;

import com.google.gwt.core.shared.SerializableThrowable;
import net.customware.gwt.dispatch.shared.DispatchException;

// wrapper for RemoteInternalException to make it serializable to browser (gwt-client) and of specific class
public class RemoteInternalDispatchException extends DispatchException {
    public RemoteInternalDispatchException() {
    }
    
    public String lsfStack;
    public String asyncStacks;
    public String javaStack;

    public RemoteInternalDispatchException(String message, String lsfStack, String asyncStacks) {
        super(message);

        this.lsfStack = lsfStack;
        this.asyncStacks = asyncStacks;
    }

    // the same as in RemoteInternalException
    public static String[] toString(Throwable e) {
        assert !(e instanceof RemoteMessageDispatchException); // should be handled before

        SerializableThrowable throwable = new SerializableThrowable("", GExceptionManager.copyMessage(e));
        GExceptionManager.copyStackTraces(e, throwable);
        String[] exStacks = getExStacks(e);
        return new String[] {throwable.getMessage(), exStacks[0], exStacks[1], exStacks[2]};
    }
    
    public static String[] getExStacks(Throwable e) {
        return new String[] {getJavaStack(e), getLsfStack(e), getAsyncStacks(e)};
    }

    public static String getJavaStack(Throwable e) {
        return e instanceof RemoteInternalDispatchException ? ((RemoteInternalDispatchException) e).javaStack : GExceptionManager.getStackTrace(e);
    }

    public static String getLsfStack(Throwable e) {
        return e instanceof RemoteInternalDispatchException ? ((RemoteInternalDispatchException) e).lsfStack : null;
    }

    public static String getAsyncStacks(Throwable e) {
        return e instanceof RemoteInternalDispatchException ? ((RemoteInternalDispatchException) e).asyncStacks : null;
    }
}
