package lsfusion.gwt.shared.exceptions;

import lsfusion.gwt.client.GExceptionManager;
import net.customware.gwt.dispatch.shared.DispatchException;

public class RemoteInternalDispatchException extends DispatchException {
    public RemoteInternalDispatchException() {
    }
    
    public String javaStack;
    public String lsfStack;

    public RemoteInternalDispatchException(Throwable cause, String javaStack, String lsfStack) {
        super(cause);

        this.javaStack = javaStack; 
        this.lsfStack = lsfStack;
    }

    // the same as in RemoteInternalException
    // returns server stacks if present     
    public static String[] getActualStacks(Throwable e) {
        String javaStack;
        String lsfStack;
        if(e instanceof RemoteInternalDispatchException) {
            javaStack = ((RemoteInternalDispatchException) e).javaStack;
            lsfStack = ((RemoteInternalDispatchException) e).lsfStack;
        } else {
            javaStack = GExceptionManager.getStackTrace(e);
            lsfStack = null;
        }
        return new String[] {javaStack, lsfStack};
    }

}
