package lsfusion.gwt.server;

import lsfusion.base.ServerUtils;
import lsfusion.gwt.client.GRequestAttemptInfo;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.BaseAction;
import lsfusion.http.provider.SessionInvalidatedException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

import static lsfusion.gwt.server.GLoggers.invocationLogger;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result> extends SimpleActionHandler<A, R> {
    protected final MainDispatchServlet servlet;

    public SimpleActionHandlerEx(MainDispatchServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public final R execute(A action, ExecutionContext context) throws DispatchException {
        try {
            String actionDetails = invocationLogger.isInfoEnabled() ? getActionDetails(action) : null;
            if (actionDetails != null)
                invocationLogger.info("Executing action" + actionDetails);

            R result = executeEx(action, context);

            if(actionDetails != null)
                invocationLogger.info("Executed action" + actionDetails);

            return result;
        } catch (RemoteException e) {
            throw new WrappedRemoteDispatchException(e); // wrap into dispatch exception to unwrap it in MainDispatchServlet
        }
    }

    public abstract R executeEx(A action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException; // last exception throws only LogicsActionHandler

    protected String getActionDetails(A action) throws SessionInvalidatedException {
        String message = " by " + ServerUtils.getAuthorizedUserName() + ": " + action.getClass().getSimpleName();

        if(action instanceof BaseAction) {
            GRequestAttemptInfo requestAttempt = ((BaseAction<?>) action).requestAttempt;
            if(requestAttempt != null)
                message += " attempt : " + requestAttempt;
        }

        return message;
    }

}
