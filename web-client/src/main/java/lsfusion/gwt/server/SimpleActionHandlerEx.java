package lsfusion.gwt.server;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.ServerMessages;
import lsfusion.base.ServerUtils;
import lsfusion.gwt.shared.exceptions.IODispatchException;
import lsfusion.gwt.shared.exceptions.MessageException;
import lsfusion.gwt.shared.exceptions.RemoteRetryException;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.RemoteMessageException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.rmi.RemoteException;

import static lsfusion.gwt.server.GLoggers.invocationLogger;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result, L extends RemoteLogicsInterface> extends SimpleActionHandler<A, R> {
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
        } catch (RemoteMessageException rme) {
            throw new MessageException(rme.getMessage());
        } catch (IOException ioe) {
            if (ioe instanceof RemoteException && !(ioe.getCause() instanceof ClassNotFoundException)) { // пробуем послать повторный запрос
                throw new RemoteRetryException(ioe.getMessage(), ioe, ExceptionUtils.getFatalRemoteExceptionCount(ioe));
            }
            throw new IODispatchException(ServerMessages.getString(servlet.getRequest(), "io.error.performing.action"), ioe);
        }
    }

    public abstract R executeEx(A action, ExecutionContext context) throws DispatchException, IOException;

    protected String getActionDetails(A action) {
        return " by " + ServerUtils.getAuthorizedUserName() + ": " + action.getClass().getSimpleName();
    }

}
