package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.navigator.ClientSettingsResult;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigator;
import lsfusion.gwt.client.controller.remote.action.navigator.InitializeNavigatorResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorInfoResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class InitializeNavigatorHandler extends NavigatorActionHandler<InitializeNavigator, InitializeNavigatorResult> {
    public InitializeNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public InitializeNavigatorResult executeEx(InitializeNavigator action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        NavigatorInfoResult navigatorInfoResult = NavigatorInfoHandler.getNavigatorInfo(remoteNavigator, servlet, getServerSettings(action));
        ClientSettingsResult clientSettingsResult = ClientSettingsHandler.getClientSettings(remoteNavigator, servlet);

        servlet.getNavigatorProvider().updateNavigatorClientSettings(action.screenSize, action.mobile);

        return new InitializeNavigatorResult(clientSettingsResult, navigatorInfoResult);
    }
}
