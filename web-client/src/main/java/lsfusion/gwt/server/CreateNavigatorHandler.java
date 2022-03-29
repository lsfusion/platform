package lsfusion.gwt.server;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.CreateNavigatorAction;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.base.exception.RemoteMessageException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.general.StringResult;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.rmi.RemoteException;

import static org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION;

public class CreateNavigatorHandler extends LogicsActionHandler<CreateNavigatorAction, StringResult> {

    public CreateNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(final CreateNavigatorAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequest(action, (sessionObject, retry) -> {
            try {
                return new StringResult(servlet.getNavigatorProvider().createNavigator(sessionObject, servlet.getRequest(), action.connectionInfo));
            } catch (RemoteMessageException e) {
                servlet.getRequest().getSession().setAttribute(AUTHENTICATION_EXCEPTION, new InternalAuthenticationServiceException(e.getMessage()));
                throw e;
            }
        });
    }

    protected String getActionDetails(CreateNavigatorAction action) throws SessionInvalidatedException {
        return super.getActionDetails(action) + " TAB IN " + servlet.getSessionInfo();
    }

}
