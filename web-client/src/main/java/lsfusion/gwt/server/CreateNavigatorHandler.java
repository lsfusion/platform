package lsfusion.gwt.server;

import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.shared.actions.CreateNavigatorAction;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.http.provider.logics.LogicsRunnable;
import lsfusion.http.provider.logics.LogicsSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.rmi.RemoteException;

public class CreateNavigatorHandler extends LogicsActionHandler<CreateNavigatorAction, StringResult> {

    public CreateNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(final CreateNavigatorAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableException {
        return runRequest(action, new LogicsRunnable<StringResult>() {
           public StringResult run(LogicsSessionObject sessionObject) throws RemoteException {
               return new StringResult(servlet.getNavigatorProvider().createNavigator(sessionObject, servlet.getRequest()));
           }
       });
    }

    protected String getActionDetails(CreateNavigatorAction action) {
        return super.getActionDetails(action) + " TAB IN " + servlet.getSessionInfo();
    }

}
