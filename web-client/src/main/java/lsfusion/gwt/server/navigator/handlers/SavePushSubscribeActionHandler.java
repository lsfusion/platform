package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.SavePushSubscribeAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SavePushSubscribeActionHandler  extends NavigatorActionHandler<SavePushSubscribeAction, VoidResult> {
    public SavePushSubscribeActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SavePushSubscribeAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        remoteNavigator.saveSubscription(action.subscription);
        return new VoidResult();
    }
}
