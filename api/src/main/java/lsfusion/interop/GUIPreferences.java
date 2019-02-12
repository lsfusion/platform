package lsfusion.interop;

import java.io.Serializable;

public class GUIPreferences implements Serializable {
    public String logicsName;
    public String logicsDisplayName;
    public byte[] logicsMainIcon;
    public byte[] logicsLogo;
    public String platformVersion;
    public Integer apiVersion;
    
    public GUIPreferences(String logicsName, String logicsDisplayName, byte[] logicsMainIcon, byte[] logicsLogo,
                          String platformVersion, Integer apiVersion) {
        this.logicsName = logicsName;
        this.logicsDisplayName = logicsDisplayName;
        this.logicsMainIcon = logicsMainIcon;
        this.logicsLogo = logicsLogo;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
    }
}
