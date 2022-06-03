package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class FileAlterationObserver extends org.apache.commons.io.monitor.FileAlterationObserver {
    private final FileChangesListener fileChangesListener;
    public FileAlterationObserver(String src, String target) {
        super(src);
        this.fileChangesListener = new FileChangesListener(src, target);
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
        public FileChangesListener(String src, String target) {
            this.src = src;
            this.target = target;
        }

        @Override
        public void onDirectoryCreate(File directory) {
            getTargetFile(directory).mkdirs();
        }

        @Override
        public void onDirectoryDelete(File directory) {
            try {
                FileUtils.deleteDirectory(getTargetFile(directory));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void onFileCreate(File file) {
            try {
                FileUtils.copyFile(file, getTargetFile(file));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void onFileChange(File file) {
            try {
                FileUtils.copyFile(file, getTargetFile(file));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public void onFileDelete(File file) {
            getTargetFile(file).delete();
        }

        private File getTargetFile(File file) {
            return Paths.get(target, file.toString().replace(src, "")).toFile();
        }
    }
}
