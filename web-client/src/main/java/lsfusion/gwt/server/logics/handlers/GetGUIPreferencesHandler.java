package lsfusion.gwt.server.logics.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.shared.actions.logics.GetGUIPreferencesAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GetGUIPreferencesHandler extends LogicsActionHandler<GetGUIPreferencesAction, StringResult> {

    public GetGUIPreferencesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetGUIPreferencesAction action, ExecutionContext context) throws DispatchException, IOException {

        String error = null;

        RemoteLogicsInterface remoteLogics = getRemoteLogics(action);
        GUIPreferences preferences = remoteLogics.getGUIPreferences();

        String serverVersion = null;
        String clientVersion = null;
        String oldPlatformVersion = BaseUtils.getPlatformVersion();
        if(oldPlatformVersion != null && !oldPlatformVersion.equals(preferences.platformVersion)) {
            serverVersion = preferences.platformVersion;
            clientVersion = oldPlatformVersion;
        } else {
            Integer oldApiVersion = BaseUtils.getApiVersion();
            if(!oldApiVersion.equals(preferences.apiVersion)) {
                serverVersion = preferences.platformVersion + " [" + preferences.apiVersion + "]";
                clientVersion = oldPlatformVersion + " [" + oldApiVersion + "]";
            }
        }

        if(serverVersion != null) {
            error = String.format(action.message, serverVersion, clientVersion);
        }

        return new StringResult(error);
    }
}