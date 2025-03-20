package lsfusion.server.base.controller.stack;

import lsfusion.base.ExceptionUtils;
import lsfusion.server.language.ScriptParsingException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

// Throwable with lsf stack
public class ThrowableWithStack {
    
    private final Throwable throwable;
    private final String lsfStack;

    public Throwable getThrowable() {
        return throwable;
    }

    public ThrowableWithStack(Throwable throwable) {
        this.throwable = throwable;
        this.lsfStack = ExecutionStackAspect.getExceptionStackTrace();
    }
    
    public String getLsfStack() {
        return lsfStack; 
    }

    public boolean isNoStackRequired() {
        return throwable instanceof ScriptParsingException;
    }
    
    public void log(String prefix, Logger logger) {
        if(isNoStackRequired()) // don't need ScriptParsingException stack (it is always the same  / doesn't matter)
            logger.error(prefix + ": " + throwable.getMessage());
        else
            logger.error(prefix + ": " + (lsfStack.isEmpty() ? "" : '\n' + lsfStack), throwable);
    }

    public String getString() {
        if(isNoStackRequired())
            return throwable.getMessage();
        else
            return ExceptionUtils.getExStackTrace(ExceptionUtils.toString(throwable), lsfStack);
    }

    @Override
    public String toString() {
        return getString();
    }

    public RemoteException propagateRemote() throws RemoteException {
        ExecutionStackAspect.setExceptionStackString(lsfStack);
        throw ExceptionUtils.propagateRemoteException(throwable);
    }
}
