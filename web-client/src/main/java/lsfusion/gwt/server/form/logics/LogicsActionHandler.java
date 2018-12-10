package lsfusion.gwt.server.form.logics;

import lsfusion.gwt.server.form.navigator.LogicsAndNavigatorActionHandler;
import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.logics.LogicsAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Result;

public abstract class LogicsActionHandler<A extends LogicsAction<R>, R extends Result> extends LogicsAndNavigatorActionHandler<A, R> {

    public LogicsActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    // shortcut's
    protected RemoteLogicsInterface getRemoteLogics(A action) {
        return getLogicsAndNavigatorSessionObject(action).remoteLogics;
    }
}
