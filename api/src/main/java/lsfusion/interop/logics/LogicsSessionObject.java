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
import java.util.Collections;
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
            Map<String, String> lsfParams = json.has("lsfParams") ? getMapFromJSONArray(json.opt("lsfParams")) : null;

            List<Pair<String, RawFileData>> loginResourcesBeforeSystem = json.has("loginResourcesBeforeSystem") ? getFileData(getMapFromJSONArray(json.opt("loginResourcesBeforeSystem"))) : null;
            List<Pair<String, RawFileData>> loginResourcesAfterSystem = json.has("loginResourcesAfterSystem") ? getFileData(getMapFromJSONArray(json.opt("loginResourcesAfterSystem"))) : null;
            List<Pair<String, RawFileData>> mainResourcesBeforeSystem = json.has("mainResourcesBeforeSystem") ? getFileData(getMapFromJSONArray(json.opt("mainResourcesBeforeSystem"))) : null;
            List<Pair<String, RawFileData>> mainResourcesAfterSystem = json.has("mainResourcesAfterSystem") ? getFileData(getMapFromJSONArray(json.opt("mainResourcesAfterSystem"))) : null;

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, platformVersion, apiVersion, inDevMode,
                    sessionConfigTimeout, anonymousUI, jnlpUrls, disableRegistration, lsfParams, loginResourcesBeforeSystem, loginResourcesAfterSystem,
                    mainResourcesBeforeSystem, mainResourcesAfterSystem);
        }
        return serverSettings;
    }

    // Expect that only JSONObject and JSONArray will be passed as param
    private Map<String, String> getMapFromJSONArray(Object json) {
        // If JSON contains only one object, it is JSONObject, if multiple objects - JSONArray.
        // If we call the optJSONArray() on JSONObject, we get null, same with optJSONObject.
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            return Collections.singletonMap(jsonObject.optString("key"), jsonObject.optString("value"));
        }

        JSONArray jsonArray = (JSONArray) json;
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (jsonObject != null)
                map.put(jsonObject.optString("key"), jsonObject.optString("value"));
        }
        return map;
    }

    private List<Pair<String, RawFileData>> getFileData(Map<String, String> files) {
        List<Pair<String, RawFileData>> resultFiles = new LinkedList<>();
        files.forEach((fileName, file) -> resultFiles.add(Pair.create(fileName, new RawFileData(new String(Base64.decodeBase64(file)).getBytes()))));
        return resultFiles;
    }

    private RawFileData getRawFileData(String base64) {
        return base64 != null ? new RawFileData(Base64Decoder.decode(base64)) : null;
    }
}
