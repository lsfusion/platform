package lsfusion.server.stack;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.server.logics.ScriptParsingException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

// Throwable with lsf stack
public class ThrowableWithStack {
    
    private final Throwable throwable;
    private final String stack;

    public Throwable getThrowable() {
        return throwable;
    }

    public ThrowableWithStack(Throwable throwable) {
        this.throwable = throwable;
        this.stack = ExecutionStackAspect.getExceptionStackString();
    }
    
    public String getStack() {
        return stack; 
    }

    public boolean isNoStackRequired() {
        return throwable instanceof ScriptParsingException;
    }
    
    public void log(String prefix, Logger logger) {
        if(isNoStackRequired()) // don't need ScriptParsingException stack (it is always the same  / doesn't matter)
            logger.error(prefix + ": " + throwable.getMessage());
        else
            logger.error(prefix + ": " + (stack.isEmpty() ? "" : '\n' + stack), throwable);
    }

    public RemoteException propagateRemote() throws RemoteException {
        ExecutionStackAspect.setExceptionStackString(stack);
        throw ExceptionUtils.propagateRemoteException(throwable);
    }
}
