package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.client.controller.remote.action.navigator.CloseNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class  CloseNavigatorHandler extends NavigatorActionHandler<CloseNavigator, VoidResult> {
    public CloseNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(CloseNavigator action, ExecutionContext context) throws RemoteException {
        removeNavigatorSessionObject(action.sessionID);
        return new VoidResult();
    }
}
