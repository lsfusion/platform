package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerExecAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class NavigatorControllerExecActionHandler extends NavigatorServerResponseActionHandler<NavigatorControllerExecAction> {
    public NavigatorControllerExecActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(NavigatorControllerExecAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).exec(action.requestIndex, action.lastReceivedRequestIndex, action.action, action.params.toArray()));
    }
}
