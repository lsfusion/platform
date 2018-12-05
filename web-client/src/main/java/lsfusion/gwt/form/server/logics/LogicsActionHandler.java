package lsfusion.gwt.form.server.logics;

import lsfusion.gwt.form.server.navigator.LogicsAndNavigatorActionHandler;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
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
