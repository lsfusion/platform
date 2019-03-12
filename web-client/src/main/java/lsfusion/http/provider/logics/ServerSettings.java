package lsfusion.http.provider.logics;

import lsfusion.base.file.RawFileData;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean anonymousUI;
    public String jnlpUrls;

    public ServerSettings(String logicsName, String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion,
                          boolean anonymousUI, String jnlpUrls) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
    }
}