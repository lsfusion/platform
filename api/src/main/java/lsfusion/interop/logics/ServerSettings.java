package lsfusion.interop.logics;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public int sessionConfigTimeout;
    public boolean anonymousUI;
    public String jnlpUrls;
    public List<Pair<String, RawFileData>> resourceFiles;
    public Set<String> filesUrls = null;
    public boolean disableRegistration;
    public Map<String, String> lsfParams;

    public ServerSettings(String logicsName, String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion,
                          int sessionConfigTimeout, boolean anonymousUI, String jnlpUrls, List<Pair<String, RawFileData>> jsFiles, boolean disableRegistration,
                          Map<String, String> lsfParams) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.sessionConfigTimeout = sessionConfigTimeout;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
        this.resourceFiles = jsFiles;
        this.disableRegistration = disableRegistration;
        this.lsfParams = lsfParams;
    }

    public synchronized void saveFiles(String appPath, String  externalResourcesParentPath) {
        if (filesUrls == null) {
            filesUrls = new HashSet<>();
            try {
                String externalResourcesAbsolutePath = appPath + "/" + externalResourcesParentPath;
                FileUtils.deleteDirectory(new File(externalResourcesAbsolutePath));
                for (Pair<String, RawFileData> pair : resourceFiles) {
                    File outputFile = new File(externalResourcesAbsolutePath, pair.first);
                    if (!outputFile.exists()) {
                        outputFile.getParentFile().mkdirs();
                        try (OutputStream out = new FileOutputStream(outputFile)) {
                            out.write(pair.second.getBytes());
                        }
                        filesUrls.add(externalResourcesParentPath + pair.first);
                    }
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    public Set<String> getFilesUrls() {
        return filesUrls;
    }
}
