package lsfusion.interop.logics;

import lsfusion.base.file.RawFileData;
import org.json.JSONObject;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean anonymousUI;
    public String jnlpUrls;
    public JSONObject jsFiles;

    public ServerSettings(String logicsName, String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion,
                          boolean anonymousUI, String jnlpUrls, JSONObject jsFiles) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
        this.jsFiles = jsFiles;
    }
}
