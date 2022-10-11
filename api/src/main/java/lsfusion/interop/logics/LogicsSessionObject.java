package lsfusion.interop.logics;

import lsfusion.base.Pair;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import org.apache.commons.net.util.Base64;
import org.castor.core.util.Base64Decoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        if(serverSettings == null || serverSettings.inDevMode || noCache) {
            ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, sessionInfo, "Service.getServerSettings[]", sessionInfo.externalRequest);

            JSONObject json = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
            String logicsName = trimToNull(json.optString("logicsName"));
            String displayName = trimToNull(json.optString("displayName"));
            RawFileData logicsLogo = getRawFileData(trimToNull(json.optString("logicsLogo")));
            RawFileData logicsIcon = getRawFileData(trimToNull(json.optString("logicsIcon")));
            String platformVersion = trimToNull(json.optString("platformVersion"));
            Integer apiVersion = json.optInt("apiVersion");
            boolean inDevMode = json.optBoolean("inDevMode");
            int sessionConfigTimeout = json.optInt("sessionConfigTimeout");
            boolean anonymousUI = json.optBoolean("anonymousUI");
            String jnlpUrls = trimToNull(json.optString("jnlpUrls"));
            if (jnlpUrls != null && contextPath != null)
                jnlpUrls = jnlpUrls.replaceAll("\\{contextPath}", contextPath);

            boolean disableRegistration = json.optBoolean("disableRegistration");
            Map<String, String> lsfParams = getMapFromJSONArray(json.optJSONArray("lsfParams"));

            List<Pair<String, RawFileData>> loginResources = getFileData(getMapFromJSONArray(json.optJSONArray("loginResources")));
            List<Pair<String, RawFileData>> mainResources = getFileData(getMapFromJSONArray(json.optJSONArray("mainResources")));

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, platformVersion, apiVersion, inDevMode,
                    sessionConfigTimeout, anonymousUI, jnlpUrls, disableRegistration, lsfParams, loginResources, mainResources);
        }
        return serverSettings;
    }

    private Map<String, String> getMapFromJSONArray(JSONArray jsonArray) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            map.put(jsonArray.optJSONObject(i).optString("key"), jsonArray.optJSONObject(i).optString("value"));
        }
        return map;
    }

    private List<Pair<String, RawFileData>> getFileData(Map<String, String> files) {
        List<Pair<String, RawFileData>> resultFiles = new LinkedList<>();
        files.forEach((fileName, file) -> resultFiles.add(Pair.create(fileName.startsWith("/") ? fileName.replaceFirst("/", "") : fileName, new RawFileData(new String(Base64.decodeBase64(file)).getBytes()))));
        return resultFiles;
    }

    private RawFileData getRawFileData(String base64) {
        return base64 != null ? new RawFileData(Base64Decoder.decode(base64)) : null;
    }
}
