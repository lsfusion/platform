package lsfusion.gwt.shared.exceptions;

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
}
