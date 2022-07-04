package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

public class SynchronizeSourcesWatcher extends FilesChangeWatcher{
    private final Map<String, String> sourceToBuildDirs;

    public SynchronizeSourcesWatcher() {
        super();
        sourceToBuildDirs = ResourceUtils.getSourceToBuildDirs();
        sourceToBuildDirs.keySet().forEach(src -> walkAndRegisterDirectories(Paths.get(src)));
    }

    @Override
    protected void processFile(WatchEvent.Kind<?> kind, File file) {
        try {
            File targetFile = getTargetFile(file);
            if (targetFile != null) {
                if (kind == ENTRY_CREATE) {
                    if (file.isDirectory())
                        targetFile.mkdirs();
                    else
                        org.apache.commons.io.FileUtils.copyFile(file, targetFile);
                } else if (kind == ENTRY_DELETE) {
                    if (file.isDirectory())
                        org.apache.commons.io.FileUtils.deleteDirectory(targetFile);
                    else
                        targetFile.delete();
                } else if (kind == ENTRY_MODIFY) {
                    FileUtils.copyFile(file, targetFile);
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private File getTargetFile(File file) {
        String src = getSrc(file.getPath());
        return src != null ? Paths.get(sourceToBuildDirs.get(src), file.toString().replace(src, "")).toFile() : null;
    }

    private String getSrc(String filePath) {
        for (String s : sourceToBuildDirs.keySet()) {
            if (filePath.replace(s, "").length() != filePath.length())
                return s;
        }
        return null;
    }
}
