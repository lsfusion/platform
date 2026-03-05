package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidNavigatorAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class VoidNavigatorActionHandler extends NavigatorServerResponseActionHandler<VoidNavigatorAction> {
    public VoidNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(VoidNavigatorAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).voidNavigatorAction(action.requestIndex, action.lastReceivedRequestIndex, action.waitRequestIndex));
    }
}
