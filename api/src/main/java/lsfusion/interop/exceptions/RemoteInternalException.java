package lsfusion.interop.exceptions;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;

import static lsfusion.base.ApiResourceBundle.getString;

// this class is needed to make throwable serializable and of specific class
public class RemoteInternalException extends RemoteServerException {
    
    private final String lsfStack;

    public RemoteInternalException(String message, String lsfStack) {
        super(message);

        this.lsfStack = lsfStack;
    }

    // the same as in RemoteInternalDispatchException
    // returns server stacks if present     
    public static Pair<String, String> getExStacks(Throwable e) {
        return new Pair<>(ExceptionUtils.getStackTrace(e), getLsfStack(e));
    }
    
    public static String getLsfStack(Throwable e) {
        return e instanceof RemoteInternalException ? ((RemoteInternalException) e).lsfStack : null;
    }
}
