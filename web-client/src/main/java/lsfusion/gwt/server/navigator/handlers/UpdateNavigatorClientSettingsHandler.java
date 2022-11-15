package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.UpdateNavigatorClientSettings;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class UpdateNavigatorClientSettingsHandler extends NavigatorActionHandler<UpdateNavigatorClientSettings, VoidResult> {
    public UpdateNavigatorClientSettingsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(UpdateNavigatorClientSettings action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        servlet.getNavigatorProvider().updateNavigatorClientSettings(action.screenSize, action.mobile);
        return new VoidResult();
    }
}
