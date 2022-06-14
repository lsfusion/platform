package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.ResourceUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class ClearCashListener extends FileAlterationListener {

    public ClearCashListener(String src) {
        super(src);
    }

    @Override
    public void onFileCreate(File file) {
        clearCaches(file, true);
    }

    @Override
    public void onFileChange(File file) {
        clearCaches(file, false);
    }

    @Override
    public void onFileDelete(File file) {
        clearCaches(file, true);
    }

    private void clearCaches(File file, boolean pathsChanged) {
        String extension = FilenameUtils.getExtension(file.getName());
        ResourceUtils.clearResourceCaches(pathsChanged, true,
                extension.equals("jar") ? s -> true : s -> s.endsWith("." + extension));
    }
}