package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.navigator.GetClientSettings;
import lsfusion.gwt.shared.actions.navigator.GetClientSettingsResult;
import lsfusion.interop.navigator.ClientSettings;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetClientSettingsHandler extends NavigatorActionHandler<GetClientSettings, GetClientSettingsResult> {
    public GetClientSettingsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetClientSettingsResult executeEx(GetClientSettings action, ExecutionContext context) throws RemoteException {
        ClientSettings clientSettings = getRemoteNavigator(action).getClientSettings();
        return new GetClientSettingsResult(clientSettings.busyDialog, clientSettings.busyDialogTimeout,
                clientSettings.devMode, clientSettings.configurationAccessAllowed, clientSettings.forbidDuplicateForms);
    }
}