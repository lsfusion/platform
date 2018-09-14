package lsfusion.interop;

import java.io.Serializable;

public class SecuritySettings implements Serializable {
    public boolean devMode;
    public boolean configurationAccessAllowed;

    public SecuritySettings(boolean devMode, boolean configurationAccessAllowed) {
        this.devMode = devMode;
        this.configurationAccessAllowed = configurationAccessAllowed;
    }
}