package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.ResourceUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.function.Predicate;

public class ClasspathChangeListener extends FileAlterationListener {

    public ClasspathChangeListener(String src) {
        super(src);
    }

    @Override
    public void onFileCreate(File file) {
        clearCaches(file, true, null);
    }

    @Override
    public void onFileChange(File file) {
        clearCaches( file, false, s -> {
            String extension = FilenameUtils.getExtension(file.getName());
            return extension.equals("jar") || s.endsWith("." + extension);
        });
    }

    @Override
    public void onFileDelete(File file) {
        clearCaches(file, true, null);
    }

    private void clearCaches(File file, boolean pathsChanged, Predicate<String> checkExtension) {
        ResourceUtils.clearResourceCaches(pathsChanged, true, checkExtension == null ? s -> s.endsWith("." + FilenameUtils.getExtension(file.getName())) : checkExtension);
    }
}