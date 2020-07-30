package lsfusion.gwt.client.controller.remote.action.navigator;

import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.exception.NonFatalHandledException;
import lsfusion.gwt.client.base.exception.StackedException;
import lsfusion.gwt.client.base.result.VoidResult;
import net.customware.gwt.dispatch.shared.DispatchException;

public class LogClientExceptionAction extends NavigatorAction<VoidResult> {
    public Throwable throwable;

    public LogClientExceptionAction() {
    }

    // result throwable class should exist on web-server
    public static Throwable fromWebClientToWebServer(Throwable t) {
        if(t instanceof DispatchException) // because that exception came from server, it will definitely be able to go back to server
            return t;
        if(t instanceof NonFatalHandledException || t instanceof StackedException) // this exception are shared
            return t;
        
        if(t instanceof StatusCodeException) {
            StackedException stackedException = GExceptionManager.getStackedException("STATUSCODE");
            if(stackedException != null)
                return stackedException;
        }
        
        Throwable webServerException = new SerializableThrowable("", GExceptionManager.copyMessage(t));
        GExceptionManager.copyStackTraces(t, webServerException);
        return webServerException;
    }

    public LogClientExceptionAction(Throwable throwable) {
        this.throwable = fromWebClientToWebServer(throwable);
    }

    @Override
    public boolean logRemoteException() {
        return false;
    }
}
