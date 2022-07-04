package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.ResourceUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.WatchEvent;

import static java.nio.file.StandardWatchEventKinds.*;

public class ClearCashWatcher extends FilesChangeWatcher {

    public ClearCashWatcher() {
        super();
    }

    @Override
    protected void processFile(WatchEvent.Kind<?> kind, File file) {
        clearCaches(file, kind == ENTRY_CREATE || kind == ENTRY_DELETE);
    }

    private void clearCaches(File file, boolean pathsChanged) {
        String extension = FilenameUtils.getExtension(file.getName());
        ResourceUtils.clearResourceCaches(pathsChanged, true, extension.equals("jar") ? s -> true : s -> s.endsWith("." + extension));
    }
}
