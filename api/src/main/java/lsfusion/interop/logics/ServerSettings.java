package lsfusion.interop.logics;

import lsfusion.base.Pair;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public FileData logicsLogo;
    public FileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean inDevMode;
    public int sessionConfigTimeout;
    public boolean anonymousUI;
    public String jnlpUrls;
    public boolean disableRegistration;
    public Map<String, String> lsfParams;
    public Map<String, String> lsfParamsAPIKeys;
    public List<Pair<String, RawFileData>> loginResourcesBeforeSystem;
    public List<Pair<String, RawFileData>> loginResourcesAfterSystem;

    public ServerSettings(String logicsName, String displayName, FileData logicsLogo, FileData logicsIcon, String platformVersion, Integer apiVersion,
                          boolean inDevMode, int sessionConfigTimeout, boolean anonymousUI, String jnlpUrls, boolean disableRegistration, Map<String, String> lsfParams,
                          List<Pair<String, RawFileData>> loginResourcesBeforeSystem, List<Pair<String, RawFileData>> loginResourcesAfterSystem) {
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
        separateAPIKeys();
    }

    private void separateAPIKeys() {
        lsfParamsAPIKeys = lsfParams.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().contains("apikey")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        lsfParams.entrySet().removeAll(lsfParamsAPIKeys.entrySet());
    }
}
