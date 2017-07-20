package lsfusion.interop;

import java.io.Serializable;

public class GUIPreferences implements Serializable {
    public String logicsName;
    public String logicsDisplayName;
    public byte[] logicsMainIcon;
    public byte[] logicsLogo;
    public boolean hideMenu;
    
    public GUIPreferences(String logicsName, String logicsDisplayName, byte[] logicsMainIcon, byte[] logicsLogo, boolean hideMenu) {
        this.logicsName = logicsName;
        this.logicsDisplayName = logicsDisplayName;
        this.logicsMainIcon = logicsMainIcon;
        this.logicsLogo = logicsLogo;
        this.hideMenu = hideMenu;
    }
}
