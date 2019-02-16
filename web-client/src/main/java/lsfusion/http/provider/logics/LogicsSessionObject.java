package lsfusion.http.provider.logics;

import lsfusion.base.ExecResult;
import lsfusion.base.FileData;
import lsfusion.client.LoginAction;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.AuthenticationToken;
import org.json.JSONObject;

import java.rmi.RemoteException;

public class LogicsSessionObject {
    
    public final RemoteLogicsInterface remoteLogics;
    
    public final LogicsConnection connection;
    
    public LogicsSessionObject(RemoteLogicsInterface remoteLogics, LogicsConnection connection) {
        this.remoteLogics = remoteLogics;
        this.connection = connection;
    }

    public JSONObject serverSettings; // caching
    public JSONObject getServerSettings() throws RemoteException {
        if(serverSettings == null) {
            ExecResult result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, LoginAction.getSessionInfo(), "System.getGUIPreferences[]", new String[]{"System.GUIPreferences[]"}, new Object[0], "utf-8", new String[0], new String[0]);
            serverSettings = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes()));
        }
        return serverSettings;
    }
    
    public String getLogicsName() throws RemoteException {
        return getServerSettings().optString("logicsName");
    }
}
