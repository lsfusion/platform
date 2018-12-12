package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.actions.navigator.GetSecuritySettings;
import lsfusion.gwt.shared.actions.navigator.GetSecuritySettingsResult;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.SecuritySettings;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetSecuritySettingsHandler extends NavigatorActionHandler<GetSecuritySettings, GetSecuritySettingsResult> {
    public GetSecuritySettingsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetSecuritySettingsResult executeEx(GetSecuritySettings action, ExecutionContext context) throws DispatchException, IOException {
        SecuritySettings securitySettings = getRemoteNavigator(action).getSecuritySettings();
        return new GetSecuritySettingsResult(securitySettings.devMode, securitySettings.configurationAccessAllowed);
    }
}