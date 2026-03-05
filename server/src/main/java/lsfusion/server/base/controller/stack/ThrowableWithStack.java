package lsfusion.server.base.controller.stack;

import lsfusion.base.ExceptionUtils;
import lsfusion.server.language.ScriptParsingException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.Arrays;

// Throwable with lsf stack
public class ThrowableWithStack {
    
    private final Throwable throwable;
    private final String lsfStack;

    public Throwable getThrowable() {
        return throwable;
    }

    public ThrowableWithStack(Throwable throwable) {
        this.throwable = packStackOverflow(throwable);
        this.lsfStack = ExecutionStackAspect.getExceptionStackTrace();
    }

    public ThrowableWithStack(Throwable throwable, String lsfStack) {
        this.throwable = packStackOverflow(throwable);
        this.lsfStack = lsfStack;
    }

    public String getLsfStack() {
        String result = lsfStack;
        if (throwable instanceof NestedThreadException)
            result += ((NestedThreadException) throwable).getAsyncLSFStacks();
        return result;
    }

    public String getJavaString() {
        String result = ExceptionUtils.toString(throwable);
        if(throwable instanceof NestedThreadException)
            result += ((NestedThreadException) throwable).getAsyncStacks();
        return result;
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
            return ExceptionUtils.getExStackTrace(getJavaString(), getLsfStack());
    }

    @Override
    public String toString() {
        return getString();
    }

    public RemoteException propagateRemote() throws RemoteException {
        ExecutionStackAspect.setExceptionStackString(lsfStack);
        throw ExceptionUtils.propagateRemoteException(throwable);
    }

    private static Throwable packStackOverflow(Throwable t) {
        int n = 1000; // now there are only top 1000 in all log4j.xml configuration files
        // however to make it actually work -XX:MaxJavaStackTraceDepth=100000 should be set larger

        StackTraceElement[] full = t.getStackTrace();
        if(!(t instanceof StackOverflowError && full.length > 1000))
            return t;

        int take = Math.max(0, Math.min(n, full.length));
        int omitted = Math.max(0, full.length - take);

        // Take the LAST N frames (the oldest part of the stack)
        StackTraceElement[] slice = Arrays.copyOfRange(full, full.length - take, full.length);

        String baseMsg = t.getMessage();
        StackOverflowError compact = new StackOverflowError(t.getClass().getSimpleName()
                + " [earliest-first, showing " + take + " frame(s), omitted " + omitted + "]"
                + (baseMsg != null ? ": " + baseMsg : ""));
        compact.setStackTrace(slice);
        return compact;
    }

}
