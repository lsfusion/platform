package lsfusion.gwt.server.logics;

import lsfusion.gwt.server.SimpleActionHandlerEx;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.logics.LogicsAction;
import lsfusion.gwt.shared.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.interop.logics.LogicsRunnable;
import net.customware.gwt.dispatch.shared.Result;

import java.rmi.RemoteException;

public abstract class LogicsActionHandler<A extends LogicsAction<R>, R extends Result> extends SimpleActionHandlerEx<A, R> {

    public LogicsActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    protected R runRequest(final A action, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        return servlet.getLogicsProvider().runRequest(action.host, action.port, action.exportName, runnable);
    }
}
