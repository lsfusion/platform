package lsfusion.gwt.server.logics.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.shared.actions.logics.CheckApiVersionAction;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class CheckApiVersionHandler extends LogicsActionHandler<CheckApiVersionAction, StringResult> {

    public CheckApiVersionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(CheckApiVersionAction action, ExecutionContext context) throws DispatchException, IOException {

        String error = null;

        RemoteLogicsInterface remoteLogics = getRemoteLogics(action);

        String serverVersion = null;
        String clientVersion = null;
        String oldPlatformVersion = BaseUtils.getPlatformVersion();
        String newPlatformVersion = remoteLogics.getPlatformVersion();
        if(oldPlatformVersion != null && !oldPlatformVersion.equals(newPlatformVersion)) {
            serverVersion = newPlatformVersion;
            clientVersion = oldPlatformVersion;
        } else {
            Integer oldApiVersion = BaseUtils.getApiVersion();
            Integer newApiVersion = remoteLogics.getApiVersion();
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