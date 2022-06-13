package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class SourcesChangeListener extends FileAlterationListener {
    private final String target;

    public SourcesChangeListener(String src, String target) {
        super(src);
        this.target = target;
    }

    public void onDirectoryCreate(File directory) {
        getTargetFile(directory).mkdirs();
    }

    @Override
    public void onDirectoryDelete(File directory) {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(getTargetFile(directory));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void onFileCreate(File file) {
        try {
            org.apache.commons.io.FileUtils.copyFile(file, getTargetFile(file));
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
        return Paths.get(target, file.toString().replace(getSrc(), "")).toFile();
    }
}

