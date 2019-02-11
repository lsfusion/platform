package lsfusion.interop;

import java.io.Serializable;

public class GUIPreferences implements Serializable {
    public String logicsName;
    public String logicsDisplayName;
    public byte[] logicsMainIcon;
    public byte[] logicsLogo;
    public boolean hideMenu;
    public String platformVersion;
    public Integer apiVersion;
    
    public GUIPreferences(String logicsName, String logicsDisplayName, byte[] logicsMainIcon, byte[] logicsLogo, boolean hideMenu,
                          String platformVersion, Integer apiVersion) {
        this.logicsName = logicsName;
        this.logicsDisplayName = logicsDisplayName;
        this.logicsMainIcon = logicsMainIcon;
        this.logicsLogo = logicsLogo;
        this.hideMenu = hideMenu;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
    }
}
