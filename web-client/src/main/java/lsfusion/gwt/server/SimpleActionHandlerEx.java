package lsfusion.gwt.server;

import lsfusion.base.ServerUtils;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableDispatchException;
import lsfusion.gwt.shared.exceptions.WrappedRemoteDispatchException;
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
        String actionDetails = invocationLogger.isInfoEnabled() ? getActionDetails(action) : null;
        if (actionDetails != null)
            invocationLogger.info("Executing action" + actionDetails);

        R result = null;
        try {
            result = executeEx(action, context);
        } catch (RemoteException e) {
            throw new WrappedRemoteDispatchException(e); // wrap into dispatch exception to unwrap it in MainDispatchServlet
        }

        if(actionDetails != null)
            invocationLogger.info("Executed action" + actionDetails);

        return result;
    }

    public abstract R executeEx(A action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException; // last exception throws only LogicsActionHandler

    protected String getActionDetails(A action) {
        return " by " + ServerUtils.getAuthorizedUserName() + ": " + action.getClass().getSimpleName();
    }

}
