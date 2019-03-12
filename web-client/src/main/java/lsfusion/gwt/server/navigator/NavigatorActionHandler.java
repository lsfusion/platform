package lsfusion.gwt.server.navigator;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.SimpleActionHandlerEx;
import lsfusion.gwt.shared.actions.navigator.CloseNavigator;
import lsfusion.gwt.shared.actions.navigator.NavigatorAction;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.navigator.callback.ClientCallBackInterface;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

public abstract class NavigatorActionHandler<A extends NavigatorAction<R>, R extends Result> extends SimpleActionHandlerEx<A, R> {

    public NavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    private NavigatorProvider getNavigatorProvider() {
        return servlet.getNavigatorProvider();
    }
    protected NavigatorSessionObject getNavigatorSessionObject(String sessionID) {
        return getNavigatorProvider().getNavigatorSessionObject(sessionID);
    }
    protected void removeNavigatorSessionObject(String sessionID) throws RemoteException {
        servlet.getFormProvider().removeFormSessionObjects(sessionID);
        getNavigatorProvider().removeNavigatorSessionObject(sessionID);
    }
    protected String getLogicsName(String sessionID) {
        return getNavigatorProvider().getLogicsName(sessionID);
    }

    // shortcut's
    protected NavigatorSessionObject getNavigatorSessionObject(A action) {
        return getNavigatorSessionObject(action.sessionID);
    }
    protected String getLogicsName(A action) {
        return getLogicsName(action.sessionID);
    }
    protected RemoteNavigatorInterface getRemoteNavigator(A action) {
        return getNavigatorSessionObject(action).remoteNavigator;
    }
    protected ClientCallBackInterface getClientCallback(A action) throws RemoteException {
        return getNavigatorSessionObject(action).getRemoteCallback();
    }

    protected String getActionDetails(A action) {
        String message = super.getActionDetails(action);

        if (action instanceof CloseNavigator) {
            message += " TAB ID " + ((CloseNavigator) action).sessionID + " IN " + servlet.getSessionInfo();
        }
        return message;
    }
}
