package lsfusion.gwt.shared.actions.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.event.shared.UmbrellaException;
import lsfusion.gwt.shared.result.VoidResult;
import net.customware.gwt.dispatch.shared.DispatchException;

public class LogClientExceptionAction extends NavigatorAction<VoidResult> {
    public String title;
    public Throwable throwable;
    
    public boolean nonFatal = false;
    public int count = 0;
    public long reqId;

    public LogClientExceptionAction() {
    }

    private static Throwable extractCause(Throwable t) {
        Throwable cause = t.getCause();
        return cause != null &&  (t instanceof UmbrellaException
                || t instanceof com.google.web.bindery.event.shared.UmbrellaException
                || t instanceof RuntimeException) ? extractCause(cause) : t;
    }

    private static Throwable toSerializable(Throwable t) {
        if(t instanceof DispatchException) // because that exception came from server, it will definitely will be able to go back to server
            return t;

        Throwable originalT = extractCause(t);
        SerializableThrowable st = new SerializableThrowable(originalT.getClass().getName(), originalT.getMessage());
        StackTraceElement[] stackTrace;
        stackTrace = originalT.getStackTrace();
        st.setStackTrace(stackTrace);
        st.setDesignatedType(originalT.getClass().getName(), true);
        return st;
    }

    public LogClientExceptionAction(String title, Throwable throwable) {
        this.title = title;
        this.throwable = toSerializable(throwable);
        assert this.throwable instanceof DispatchException || this.throwable instanceof SerializableThrowable;  
    }

    public LogClientExceptionAction(String title, Throwable throwable, int count, long reqId) {
        this(title, throwable);

        nonFatal = true;
        this.count = count;
        this.reqId = reqId;
    }

    @Override
    public boolean logRemoteException() {
        return false;
    }
}
