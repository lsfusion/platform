package lsfusion.gwt.shared.exceptions;

import com.google.gwt.core.shared.SerializableThrowable;
import lsfusion.base.ExceptionUtils;
import lsfusion.gwt.client.GExceptionManager;
import net.customware.gwt.dispatch.shared.DispatchException;

// wrapper for RemoteInternalException to make it serializable to browser (gwt-client) and of specific class
public class RemoteInternalDispatchException extends DispatchException {
    public RemoteInternalDispatchException() {
    }
    
    public String lsfStack;

    public RemoteInternalDispatchException(String message, String lsfStack) {
        super(message);

        this.lsfStack = lsfStack;
    }

    // the same as in RemoteInternalException
    public static String[] toString(Throwable e) {
        SerializableThrowable throwable = new SerializableThrowable("", GExceptionManager.copyMessage(e));
        ExceptionUtils.copyStackTraces(e, throwable);
        String[] exStacks = getExStacks(throwable);
        return new String[] {throwable.getMessage(), exStacks[0], exStacks[1]};
    }
    
    public static String[] getExStacks(Throwable e) {
        return new String[] {GExceptionManager.getStackTrace(e), getLsfStack(e)};
    }

    public static String getLsfStack(Throwable e) {
        return e instanceof RemoteInternalDispatchException ? ((RemoteInternalDispatchException) e).lsfStack : null;
    }

}
