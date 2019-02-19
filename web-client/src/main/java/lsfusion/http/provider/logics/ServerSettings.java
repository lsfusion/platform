package lsfusion.http.provider.logics;

import lsfusion.base.RawFileData;

public class ServerSettings {
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;

    public ServerSettings(String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion) {
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
    }
}