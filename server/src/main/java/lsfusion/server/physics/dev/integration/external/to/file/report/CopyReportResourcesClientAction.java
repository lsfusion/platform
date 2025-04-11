package lsfusion.server.physics.dev.integration.external.to.file.report;

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
    public final String logicsName;
    public final FileData zipFile;
    public final String md5;

    public CopyReportResourcesClientAction(String logicsName, FileData zipFile, String md5) {
        this.logicsName = logicsName;
        this.zipFile = zipFile;
        this.md5 = md5;
    }

    private File getJasperFontsTempDir() {
        return new File(new File(SystemUtils.getUserDir(), logicsName), "jasper-fonts");
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            File jar = new File(getJasperFontsTempDir(), md5 + ".jar");
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
            return e.getMessage();
        }
    }

    private void setContextClassLoader(File jar) throws IOException {
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jar.toURI().toURL()}, Thread.currentThread().getContextClassLoader()));
    }

    private void safeDeleteOldFiles(String jar) {
        File[] listFiles = getJasperFontsTempDir().listFiles(f -> !f.getName().equals(jar));
        if (listFiles != null) {
            for (File file : listFiles) {
                BaseUtils.safeDelete(file);
            }
        }
    }
}