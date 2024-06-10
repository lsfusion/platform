package lsfusion.interop.logics;

import lsfusion.base.Pair;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;

import java.util.List;
import java.util.Map;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public FileData logicsLogo;
    public FileData logicsIcon;
    public FileData PWAIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean inDevMode;
    public int sessionConfigTimeout;
    public boolean anonymousUI;
    public String jnlpUrls;
    public boolean disableRegistration;
    public Map<String, String> lsfParams;
    public List<Pair<String, RawFileData>> noAuthResourcesBeforeSystem;
    public List<Pair<String, RawFileData>> noAuthResourcesAfterSystem;

    public ServerSettings(String logicsName, String displayName, FileData logicsLogo, FileData logicsIcon, FileData PWAIcon, String platformVersion, Integer apiVersion,
                          boolean inDevMode, int sessionConfigTimeout, boolean anonymousUI, String jnlpUrls, boolean disableRegistration, Map<String, String> lsfParams,
                          List<Pair<String, RawFileData>> noAuthResourcesBeforeSystem, List<Pair<String, RawFileData>> noAuthResourcesAfterSystem) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.PWAIcon = PWAIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.inDevMode = inDevMode;
        this.sessionConfigTimeout = sessionConfigTimeout;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
        this.disableRegistration = disableRegistration;
        this.lsfParams = lsfParams;
        this.noAuthResourcesBeforeSystem = noAuthResourcesBeforeSystem;
        this.noAuthResourcesAfterSystem = noAuthResourcesAfterSystem;
    }
}
