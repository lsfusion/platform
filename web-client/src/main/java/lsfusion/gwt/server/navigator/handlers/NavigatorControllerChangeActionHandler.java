package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerChangeAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class NavigatorControllerChangeActionHandler extends NavigatorServerResponseActionHandler<NavigatorControllerChangeAction> {
    public NavigatorControllerChangeActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(NavigatorControllerChangeAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).change(action.requestIndex, action.lastReceivedRequestIndex, action.property, action.params.toArray(), action.value));
    }
}
