package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ThrowInNavigatorAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ThrowInNavigatorActionHandler extends NavigatorServerResponseActionHandler<ThrowInNavigatorAction> {
    public ThrowInNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInNavigatorAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).throwInServerInvocation(action.requestIndex, action.lastReceivedRequestIndex, action.continueIndex, LogClientExceptionActionHandler.fromWebServerToAppServer(action.throwable)));
    }
}
