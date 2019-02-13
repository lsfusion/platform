package lsfusion.gwt.server.logics;

import lsfusion.gwt.server.navigator.LogicsAndNavigatorActionHandler;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.logics.LogicsAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class LogicsActionHandler<A extends LogicsAction<R>, R extends Result> extends LogicsAndNavigatorActionHandler<A, R> {

    public LogicsActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    // shortcut's
    protected RemoteLogicsInterface getRemoteLogics(A action) {
        return getLogicsAndNavigatorSessionObject(action).remoteLogics;
    }
}
