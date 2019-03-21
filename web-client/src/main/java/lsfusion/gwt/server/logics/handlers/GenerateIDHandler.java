package lsfusion.gwt.server.logics.handlers;

import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.logics.GenerateID;
import lsfusion.gwt.shared.actions.logics.GenerateIDResult;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GenerateIDHandler extends LogicsActionHandler<GenerateID, GenerateIDResult> {
    public GenerateIDHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequest(action, new LogicsRunnable<GenerateIDResult>() {
            public GenerateIDResult run(LogicsSessionObject sessionObject) throws RemoteException {
                return new GenerateIDResult(sessionObject.remoteLogics.generateID());
            }
        });
    }
}
