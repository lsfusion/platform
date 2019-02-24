package lsfusion.gwt.shared.exceptions;

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
    // returns server stacks if present     
    public static String[] getActualStacks(Throwable e) {
        return new String[] {GExceptionManager.getStackTrace(e), getActualLsfStack(e)};
    }

    public static String getActualLsfStack(Throwable e) {
        return e instanceof RemoteInternalDispatchException ? ((RemoteInternalDispatchException) e).lsfStack : null;
    }

}
