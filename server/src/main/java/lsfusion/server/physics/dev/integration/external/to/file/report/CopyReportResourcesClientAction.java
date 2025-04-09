package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class CopyReportResourcesClientAction implements ClientAction {
    public final FileData zipFile;
    public final String md5;

    public CopyReportResourcesClientAction(FileData zipFile, String md5) {
        this.zipFile = zipFile;
        this.md5 = md5;
    }

    public final File jasperFontsTempDir = new File(SystemUtils.getUserDir(), "jasper-fonts");

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            File jar = new File(jasperFontsTempDir, md5 + ".jar");
            if (zipFile == null) {
                boolean exists = jar.exists();
                if (exists)
                    setContextClassLoader(jar);
                return exists;
            } else {
                zipFile.getRawFile().write(jar);
                setContextClassLoader(jar);
                //If server restarts while the client is running, the jar file cannot be deleted before setContextClassLoader with new jar.
                //Need first execute setContextClassLoader with new jar and only then delete old jar.
                safeDeleteOldFiles(jar.getName());
                return null;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void setContextClassLoader(File jar) throws IOException {
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jar.toURI().toURL()}, Thread.currentThread().getContextClassLoader()));
    }

    private void safeDeleteOldFiles(String jar) {
        File[] listFiles = jasperFontsTempDir.listFiles(f -> !f.getName().equals(jar));
        if (listFiles != null) {
            for (File file : listFiles) {
                BaseUtils.safeDelete(file);
            }
        }
    }
}