package lsfusion.interop.logics;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import org.castor.core.util.Base64Decoder;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
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
    public ServerSettings getServerSettings(SessionInfo sessionInfo, String contextPath, boolean noCache) throws RemoteException {
        if(serverSettings == null || noCache) {
            ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, sessionInfo, "Service.getServerSettings[]", new ExternalRequest(sessionInfo.query));

            JSONObject json = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
            String logicsName = trimToNull(json.optString("logicsName"));
            String displayName = trimToNull(json.optString("displayName"));
            RawFileData logicsLogo = getRawFileData(trimToNull(json.optString("logicsLogo")));
            RawFileData logicsIcon = getRawFileData(trimToNull(json.optString("logicsIcon")));
            String platformVersion = trimToNull(json.optString("platformVersion"));
            Integer apiVersion = json.optInt("apiVersion");
            boolean anonymousUI = json.optBoolean("anonymousUI");
            String jnlpUrls = trimToNull(json.optString("jnlpUrls"));
            if (jnlpUrls != null && contextPath != null) {
                jnlpUrls = jnlpUrls.replaceAll("\\{contextPath}", contextPath);
            }
            JSONObject jsFiles = null;
            if (json.has("jsFiles")){
                jsFiles = new JSONObject(json.optString("jsFiles"));
            }

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, platformVersion, apiVersion, anonymousUI, jnlpUrls, jsFiles);
        }
        return serverSettings;
    }

    private RawFileData getRawFileData(String base64) {
        return base64 != null ? new RawFileData(Base64Decoder.decode(base64)) : null;
    }

    public String getLogicsName(SessionInfo sessionInfo) throws RemoteException {
        return getServerSettings(sessionInfo, null, false).logicsName;
    }
}
