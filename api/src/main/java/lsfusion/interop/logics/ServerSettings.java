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
import java.util.Set;

public class ServerSettings {
    public String logicsName;
    public String displayName;
    public RawFileData logicsLogo;
    public RawFileData logicsIcon;
    public String platformVersion;
    public Integer apiVersion;
    public boolean anonymousUI;
    public String jnlpUrls;
    public List<Pair<String, RawFileData>> resourceFiles;
    public Set<String> filesUrls = null;

    public ServerSettings(String logicsName, String displayName, RawFileData logicsLogo, RawFileData logicsIcon, String platformVersion, Integer apiVersion,
                          boolean anonymousUI, String jnlpUrls, List<Pair<String, RawFileData>> jsFiles) {
        this.logicsName = logicsName;
        this.displayName = displayName;
        this.logicsLogo = logicsLogo;
        this.logicsIcon = logicsIcon;
        this.platformVersion = platformVersion;
        this.apiVersion = apiVersion;
        this.anonymousUI = anonymousUI;
        this.jnlpUrls = jnlpUrls;
        this.resourceFiles = jsFiles;
    }

    public synchronized void saveFiles(String appPath, String  externalResourcesParentPath) {
        if (filesUrls == null) {
            filesUrls = new HashSet<>();
            try {
                FileUtils.deleteDirectory(new File(appPath + externalResourcesParentPath));
                for (Pair<String, RawFileData> pair : resourceFiles) {
                    String folderPath = appPath + externalResourcesParentPath + pair.first.split("/")[0];
                    File outputFile = new File(appPath + externalResourcesParentPath, pair.first);
                    if (!outputFile.exists()) {
                        new File(folderPath).mkdirs();
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
