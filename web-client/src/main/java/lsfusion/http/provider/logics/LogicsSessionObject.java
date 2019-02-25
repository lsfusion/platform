package lsfusion.http.provider.logics;

import lsfusion.base.ExternalRequest;
import lsfusion.base.ExternalResponse;
import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.AuthenticationToken;
import org.castor.core.util.Base64Decoder;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

import static lsfusion.base.BaseUtils.trimToNull;

public class LogicsSessionObject {
    
    public final RemoteLogicsInterface remoteLogics;
    
    public final LogicsConnection connection;
    
    public LogicsSessionObject(RemoteLogicsInterface remoteLogics, LogicsConnection connection) {
        this.remoteLogics = remoteLogics;
        this.connection = connection;
    }

    public ServerSettings serverSettings; // caching
    public ServerSettings getServerSettings(HttpServletRequest request) throws RemoteException {
        if(serverSettings == null) {
            ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getSessionInfo(request), "Service.getServerSettings[]", new ExternalRequest());

            JSONObject json = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes()));
            String logicsName = trimToNull(json.optString("logicsName"));
            String displayName = trimToNull(json.optString("displayName"));
            RawFileData logicsLogo = getRawFileData(trimToNull(json.optString("logicsLogo")));
            RawFileData logicsIcon = getRawFileData(trimToNull(json.optString("logicsIcon")));
            String platformVersion = trimToNull(json.optString("platformVersion"));
            Integer apiVersion = json.optInt("apiVersion");

            boolean anonymousUI = json.optBoolean("anonymousUI");

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, platformVersion, apiVersion, anonymousUI);
        }
        return serverSettings;
    }
    
    public String jnlpUrls;
    public String getJnlpUrls(HttpServletRequest request) throws RemoteException {
        if (jnlpUrls == null) {
            ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, NavigatorProviderImpl.getSessionInfo(request), "Security.generateJnlpUrls[]", new ExternalRequest());
            
            jnlpUrls = (String) result.results[0];
            if (jnlpUrls != null) {
                jnlpUrls = jnlpUrls.replaceAll("\\{contextPath}", request.getContextPath());
            }
        }
        return jnlpUrls;
    }

    private RawFileData getRawFileData(String base64) {
        return base64 != null ? new RawFileData(Base64Decoder.decode(base64)) : null;
    }

    public String getLogicsName(HttpServletRequest request) throws RemoteException {
        return getServerSettings(request).logicsName;
    }
}
