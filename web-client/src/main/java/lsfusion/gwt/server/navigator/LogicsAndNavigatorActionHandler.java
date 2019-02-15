package lsfusion.gwt.server.navigator;

import lsfusion.gwt.server.SimpleActionHandlerEx;
import lsfusion.http.provider.navigator.LogicsAndNavigatorProvider;
import lsfusion.http.provider.navigator.LogicsAndNavigatorSessionObject;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.navigator.LogicsAndNavigatorAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

public abstract class LogicsAndNavigatorActionHandler<A extends LogicsAndNavigatorAction<R>, R extends Result> extends SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    public LogicsAndNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    private LogicsAndNavigatorProvider getLogicsAndNavigatorProvider() {
        return servlet.getLogicsAndNavigatorProvider();
    }
    protected LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(String sessionID) {
        return getLogicsAndNavigatorProvider().getLogicsAndNavigatorSessionObject(sessionID);
    }
    protected void removeLogicsAndNavigatorSessionObject(String sessionID) throws RemoteException {
        servlet.getFormProvider().removeFormSessionObjects(sessionID);
        getLogicsAndNavigatorProvider().removeLogicsAndNavigatorSessionObject(sessionID);
    }
    protected String getLogicsName(String sessionID) {
        return getLogicsAndNavigatorProvider().getLogicsName(sessionID);
    }

    // shortcut's
    protected LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(A action) {
        return getLogicsAndNavigatorSessionObject(action.sessionID);
    }
    protected String getLogicsName(A action) {
        return getLogicsAndNavigatorProvider().getLogicsName(action.sessionID);
    }
}
