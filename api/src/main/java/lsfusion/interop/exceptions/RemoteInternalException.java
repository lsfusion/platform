package lsfusion.interop.exceptions;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {
    
    private final String javaStack;
    private final String lsfStack;

    public RemoteInternalException(String message, Throwable cause, boolean isNoStackRequired, String lsfStack) {
        this(message + ": " + cause.getMessage(), isNoStackRequired ? null : ExceptionUtils.getStackTrace(cause), isNoStackRequired ? null : lsfStack);
    }

    public RemoteInternalException(Throwable cause, String javaStack, String lsfStack) {
        this(cause.getMessage(), javaStack, lsfStack);
    }
    public RemoteInternalException(String message, String javaStack, String lsfStack) {
        super(message); // we can't set pass the cause further, because its class can be missing at client side
        
        this.javaStack = javaStack;
        this.lsfStack = lsfStack;
    }
    
    // returns server stacks if present     
    public static Pair<String, String> getActualStacks(Throwable e) {
        String javaStack;
        String lsfStack;
        if(e instanceof RemoteInternalException) {
            javaStack = ((RemoteInternalException) e).javaStack;
            lsfStack = ((RemoteInternalException) e).lsfStack; 
        } else {
            javaStack = ExceptionUtils.getStackTrace(e);
            lsfStack = null;
        }
        return new Pair<>(javaStack, lsfStack);
    }
}
