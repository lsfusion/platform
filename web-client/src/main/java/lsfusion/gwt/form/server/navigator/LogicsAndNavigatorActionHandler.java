package lsfusion.gwt.form.server.navigator;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorProvider;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.LogicsAndNavigatorAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

public abstract class LogicsAndNavigatorActionHandler<A extends LogicsAndNavigatorAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    public LogicsAndNavigatorActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    private LogicsAndNavigatorProvider getLogicsAndNavigatorProvider() {
        return ((LSFusionDispatchServlet)servlet).getLogicsAndNavigatorProvider();
    }
    protected LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(String sessionID) {
        return getLogicsAndNavigatorProvider().getLogicsAndNavigatorSessionObject(sessionID);
    }
    protected void removeLogicsAndNavigatorSessionObject(String navigatorID, FormProvider formProvider) throws RemoteException {
        LogicsAndNavigatorSessionObject logicsAndNavigatorSessionObject = getLogicsAndNavigatorProvider().removeLogicsAndNavigatorSessionObject(navigatorID);
        formProvider.removeFormSessionObjects(navigatorID);
        logicsAndNavigatorSessionObject.remoteNavigator.close(); // we d
    }

    // shortcut's
    protected LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(A action) {
        return getLogicsAndNavigatorSessionObject(action.sessionID);
    }

}
