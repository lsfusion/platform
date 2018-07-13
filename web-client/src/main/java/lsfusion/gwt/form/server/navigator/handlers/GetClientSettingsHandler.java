package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetClientSettings;
import lsfusion.gwt.form.shared.actions.navigator.GetClientSettingsResult;
import lsfusion.interop.ClientSettings;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetClientSettingsHandler extends LoggableActionHandler<GetClientSettings, GetClientSettingsResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public GetClientSettingsHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public GetClientSettingsResult executeEx(GetClientSettings action, ExecutionContext context) throws DispatchException, IOException {
        ClientSettings clientSettings = servlet.getNavigator().getClientSettings();
        return new GetClientSettingsResult(clientSettings.busyDialog, clientSettings.busyDialogTimeout);
    }
}