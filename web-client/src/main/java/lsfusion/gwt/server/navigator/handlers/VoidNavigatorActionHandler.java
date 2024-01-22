package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidNavigatorAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class VoidNavigatorActionHandler extends NavigatorActionHandler<VoidNavigatorAction, VoidResult> {
    public VoidNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(VoidNavigatorAction action, ExecutionContext context) throws RemoteException {
        getRemoteNavigator(action).voidNavigatorAction(action.requestIndex, action.lastReceivedRequestIndex, action.waitRequestIndex);
        return new VoidResult();
    }
}
