package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetSecuritySettings;
import lsfusion.gwt.form.shared.actions.navigator.GetSecuritySettingsResult;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.SecuritySettings;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetSecuritySettingsHandler extends LoggableActionHandler<GetSecuritySettings, GetSecuritySettingsResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public GetSecuritySettingsHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public GetSecuritySettingsResult executeEx(GetSecuritySettings action, ExecutionContext context) throws DispatchException, IOException {
        SecuritySettings securitySettings = servlet.getNavigator().getSecuritySettings();
        return new GetSecuritySettingsResult(securitySettings.devMode, securitySettings.configurationAccessAllowed);
    }
}