package lsfusion.server.stack;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
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
    
    public void log(String prefix, Logger logger) {
        logger.error(prefix + ": " + (stack.isEmpty() ? "" : '\n' + stack), throwable);
    }

    public RemoteException propagateRemote() throws RemoteException {
        ExecutionStackAspect.setExceptionStackString(stack);
        throw ExceptionUtils.propagateRemoteException(throwable);
    }
}
