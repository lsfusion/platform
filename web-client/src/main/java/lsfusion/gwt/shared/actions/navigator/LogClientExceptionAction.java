package lsfusion.gwt.shared.actions.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.event.shared.UmbrellaException;
import lsfusion.gwt.client.GExceptionManager;
import lsfusion.gwt.shared.exceptions.NonFatalHandledException;
import lsfusion.gwt.shared.result.VoidResult;
import net.customware.gwt.dispatch.shared.DispatchException;

public class LogClientExceptionAction extends NavigatorAction<VoidResult> {
    public String title;
    public Throwable throwable;

    public LogClientExceptionAction() {
    }

    private static Throwable extractCause(Throwable t) {
        Throwable cause = t.getCause();
        return cause != null &&  (t instanceof UmbrellaException
                || t instanceof com.google.web.bindery.event.shared.UmbrellaException
                || t instanceof RuntimeException) ? extractCause(cause) : t;
    }

    // result throwable class should exist on web-server
    private static Throwable fromWebClientToWebServer(Throwable t) {
        if(t instanceof DispatchException) // because that exception came from server, it will definitely be able to go back to server
            return t;
        if(t instanceof NonFatalHandledException) // this exception is shared
            return t;

        Throwable originalT = extractCause(t);
        Throwable webServerException = GExceptionManager.copyMessage(originalT);
        GExceptionManager.copyStackTraces(originalT, webServerException);
        return webServerException;
    }

    public LogClientExceptionAction(String title, Throwable throwable) {
        this.title = title;
        this.throwable = fromWebClientToWebServer(throwable);
        assert this.throwable instanceof DispatchException || this.throwable instanceof NonFatalHandledException || this.throwable instanceof SerializableThrowable;  
    }

    @Override
    public boolean logRemoteException() {
        return false;
    }
}
