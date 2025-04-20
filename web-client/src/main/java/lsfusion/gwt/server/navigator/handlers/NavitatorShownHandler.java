package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigator;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigatorResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorShown;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class NavitatorShownHandler extends NavigatorActionHandler<NavigatorShown, VoidResult> {

    public NavitatorShownHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(NavigatorShown action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        getNavigatorSessionObject(action).initialized = 2;
        return new VoidResult();
    }
}
