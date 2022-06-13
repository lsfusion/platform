package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileAlterationObserver extends org.apache.commons.io.monitor.FileAlterationObserver {
    private final FileChangesListener fileChangesListener;
    public FileAlterationObserver(String src, String target, String... extensions) {
        super(src);
        this.fileChangesListener = new FileChangesListener(src, target, extensions);
        init();
    }

    private void init() {
        try {
            addListener(fileChangesListener);
            initialize();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static class FileChangesListener extends FileAlterationListenerAdaptor {
        private final String src;
        private final String target;
        private final String[] extensions;
        public FileChangesListener(String src, String target, String... extensions) {
            this.src = src;
            this.target = target;
            this.extensions = extensions;
        }

        @Override
        public void onDirectoryCreate(File directory) {
            if (target != null)
                getTargetFile(directory).mkdirs();
        }

        @Override
        public void onDirectoryDelete(File directory) {
            if (target != null) {
                try {
                    FileUtils.deleteDirectory(getTargetFile(directory));
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        }

        @Override
        public void onFileCreate(File file) {
            try {
                if (!clearCaches(file, true))
                    FileUtils.copyFile(file, getTargetFile(file));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void onFileChange(File file) {
            try {
                if (!clearCaches(file, false))
                    FileUtils.copyFile(file, getTargetFile(file));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void onFileDelete(File file) {
            if (!clearCaches(file, true))
                getTargetFile(file).delete();
        }

        private boolean clearCaches(File file, boolean pathsChanged) {
            if (extensions.length > 0) {
                Arrays.stream(extensions).filter(extension -> FilenameUtils.getExtension(file.getName()).equals(extension))
                        .forEach(extension -> ResourceUtils.clearResourceCaches(extension, pathsChanged, true));
                return true;
            }
            return false;
        }

        private File getTargetFile(File file) {
            return Paths.get(target, file.toString().replace(src, "")).toFile();
        }
    }
}
