package lsfusion.gwt.server.navigator;

import lsfusion.gwt.client.controller.remote.action.navigator.CloseNavigator;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.SimpleActionHandlerEx;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

public abstract class NavigatorActionHandler<A extends NavigatorAction<R>, R extends Result> extends SimpleActionHandlerEx<A, R> {

    public NavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    private NavigatorProvider getNavigatorProvider() {
        return servlet.getNavigatorProvider();
    }
    protected NavigatorSessionObject getNavigatorSessionObject(String sessionID) throws SessionInvalidatedException {
        return getNavigatorProvider().getNavigatorSessionObject(sessionID);
    }
    protected void removeNavigatorSessionObject(String sessionID) throws RemoteException {
        servlet.getFormProvider().removeFormSessionObjects(sessionID);
        getNavigatorProvider().removeNavigatorSessionObject(sessionID);
    }
    protected ServerSettings getServerSettings(String sessionID) throws SessionInvalidatedException {
        return getNavigatorProvider().getServerSettings(sessionID);
    }

    // shortcut's
    protected NavigatorSessionObject getNavigatorSessionObject(A action) throws SessionInvalidatedException {
        return getNavigatorSessionObject(action.sessionID);
    }
    protected ServerSettings getServerSettings(A action) throws SessionInvalidatedException {
        return getServerSettings(action.sessionID);
    }
    protected RemoteNavigatorInterface getRemoteNavigator(A action) throws SessionInvalidatedException {
        return getNavigatorSessionObject(action).remoteNavigator;
    }
    protected ClientCallBackInterface getClientCallback(A action) throws RemoteException {
        return getNavigatorSessionObject(action).getRemoteCallback();
    }

    protected String getActionDetails(A action) throws SessionInvalidatedException {
        String message = super.getActionDetails(action);

        if (action instanceof CloseNavigator) {
            message += " TAB ID " + ((CloseNavigator) action).sessionID + " IN " + servlet.getSessionInfo();
        }
        return message;
    }
}
