package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.CheckApiVersionAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class CheckApiVersionHandler extends LoggableActionHandler<CheckApiVersionAction, StringResult, RemoteLogicsInterface> implements NavigatorActionHandler {

    public CheckApiVersionHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(CheckApiVersionAction action, ExecutionContext context) throws DispatchException, IOException {

        String error = null;

        String serverVersion = null;
        String clientVersion = null;
        String oldPlatformVersion = BaseUtils.getPlatformVersion();
        String newPlatformVersion = servlet.getLogics().getPlatformVersion();
        if(oldPlatformVersion != null && !oldPlatformVersion.equals(newPlatformVersion)) {
            serverVersion = newPlatformVersion;
            clientVersion = oldPlatformVersion;
        } else {
            Integer oldApiVersion = BaseUtils.getApiVersion();
            Integer newApiVersion = servlet.getLogics().getApiVersion();
            if(!oldApiVersion.equals(newApiVersion)) {
                serverVersion = newPlatformVersion + " [" + newApiVersion + "]";
                clientVersion = oldPlatformVersion + " [" + oldApiVersion + "]";
            }
        }

        if(serverVersion != null) {
            error = String.format(action.message, serverVersion, clientVersion);
        }

        return new StringResult(error);
    }
}