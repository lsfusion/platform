package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.client.controller.remote.action.navigator.InterruptNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class InterruptNavigatorHandler extends NavigatorActionHandler<InterruptNavigator, VoidResult> {

    public InterruptNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(InterruptNavigator action, ExecutionContext context) throws RemoteException {
        getRemoteNavigator(action).interrupt(action.cancelable);
        return new VoidResult();
    }
}