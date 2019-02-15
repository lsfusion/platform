package lsfusion.gwt.server.logics.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExecResult;
import lsfusion.base.FileData;
import lsfusion.client.LoginAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.shared.actions.logics.GetGUIPreferencesAction;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.AuthenticationToken;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import org.json.JSONObject;

import java.io.IOException;

public class GetGUIPreferencesHandler extends LogicsActionHandler<GetGUIPreferencesAction, StringResult> {

    public GetGUIPreferencesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetGUIPreferencesAction action, ExecutionContext context) throws DispatchException, IOException {

        String error = null;

        RemoteLogicsInterface remoteLogics = getRemoteLogics(action);
        ExecResult result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, LoginAction.getSessionInfo(), "System.getGUIPreferences[]", new String[] { "System.GUIPreferences[]"}, new Object[0], "utf-8", new String[0], new String[0]);
        JSONObject preferences = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes()));

        String serverPlatformVersion = preferences.optString("platformVersion");
        Integer serverApiVersion = preferences.optInt("apiVersion");

        String serverVersion = null;
        String clientVersion = null;
        String oldPlatformVersion = BaseUtils.getPlatformVersion();
        if(oldPlatformVersion != null && !oldPlatformVersion.equals(serverPlatformVersion)) {
            serverVersion = serverPlatformVersion;
            clientVersion = oldPlatformVersion;
        } else {
            Integer oldApiVersion = BaseUtils.getApiVersion();
            if(!oldApiVersion.equals(serverApiVersion)) {
                serverVersion = serverPlatformVersion + " [" + serverApiVersion + "]";
                clientVersion = oldPlatformVersion + " [" + oldApiVersion + "]";
            }
        }

        if(serverVersion != null) {
            error = String.format(action.message, serverVersion, clientVersion);
        }

        return new StringResult(error);
    }
}