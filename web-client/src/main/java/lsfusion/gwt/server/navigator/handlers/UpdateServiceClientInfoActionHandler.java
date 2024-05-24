package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.UpdateServiceClientInfoAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class UpdateServiceClientInfoActionHandler extends NavigatorActionHandler<UpdateServiceClientInfoAction, VoidResult> {
    public UpdateServiceClientInfoActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(UpdateServiceClientInfoAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        getRemoteNavigator(action).updateServiceClientInfo(action.subscription, action.clientId);
        return new VoidResult();
    }
}
