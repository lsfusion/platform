package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.shared.actions.navigator.GetClientSettings;
import lsfusion.gwt.shared.actions.navigator.GetClientSettingsResult;
import lsfusion.interop.ClientSettings;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetClientSettingsHandler extends NavigatorActionHandler<GetClientSettings, GetClientSettingsResult> {
    public GetClientSettingsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetClientSettingsResult executeEx(GetClientSettings action, ExecutionContext context) throws DispatchException, IOException {
        ClientSettings clientSettings = getRemoteNavigator(action).getClientSettings();
        return new GetClientSettingsResult(clientSettings.busyDialog, clientSettings.busyDialogTimeout, clientSettings.configurationAccessAllowed);
    }
}