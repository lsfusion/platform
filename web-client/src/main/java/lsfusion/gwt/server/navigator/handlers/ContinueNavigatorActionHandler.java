package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ContinueNavigatorAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ContinueNavigatorActionHandler extends NavigatorServerResponseActionHandler<ContinueNavigatorAction> {
    private final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ContinueNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ContinueNavigatorAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).continueServerInvocation(action.requestIndex, action.lastReceivedRequestIndex, action.continueIndex, gwtConverter.convertOrCast(action.actionResult)));
    }
}
