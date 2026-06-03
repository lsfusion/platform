package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerEvalAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class NavigatorControllerEvalActionHandler extends NavigatorServerResponseActionHandler<NavigatorControllerEvalAction> {
    public NavigatorControllerEvalActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(NavigatorControllerEvalAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).eval(action.requestIndex, action.lastReceivedRequestIndex, action.callbackId, action.script, action.params.toArray()));
    }
}
