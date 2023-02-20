package lsfusion.interop.logics;

import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;

import java.util.List;
import java.util.Map;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean inDevMode;
    public int sessionConfigTimeout;
    public boolean anonymousUI;
    public String jnlpUrls;
    public boolean disableRegistration;
    public Map<String, String> lsfParams;
    public List<Pair<String, RawFileData>> loginResourcesBeforeSystem;
    public List<Pair<String, RawFileData>> loginResourcesAfterSystem;
    public List<Pair<String, RawFileData>> mainResourcesBeforeSystem;
    public List<Pair<String, RawFileData>> mainResourcesAfterSystem;

    public ServerSettings(String logicsName, String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion,
                          boolean inDevMode, int sessionConfigTimeout, boolean anonymousUI, String jnlpUrls, boolean disableRegistration, Map<String, String> lsfParams,
                          List<Pair<String, RawFileData>> loginResourcesBeforeSystem, List<Pair<String, RawFileData>> loginResourcesAfterSystem,
                          List<Pair<String, RawFileData>> mainResourcesBeforeSystem, List<Pair<String, RawFileData>> mainResourcesAfterSystem) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.inDevMode = inDevMode;
        this.sessionConfigTimeout = sessionConfigTimeout;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
        this.disableRegistration = disableRegistration;
        this.lsfParams = lsfParams;
        this.loginResourcesBeforeSystem = loginResourcesBeforeSystem;
        this.loginResourcesAfterSystem = loginResourcesAfterSystem;
        this.mainResourcesBeforeSystem = mainResourcesBeforeSystem;
        this.mainResourcesAfterSystem = mainResourcesAfterSystem;
    }
}
