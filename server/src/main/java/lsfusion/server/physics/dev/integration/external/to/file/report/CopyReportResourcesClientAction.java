package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.form.print.ReportExtensionsRegistryFactory;
import net.lingala.zip4j.ZipFile;
import net.sf.jasperreports.engine.JRPropertiesMap;
import org.apache.commons.io.FileUtils;

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

    public final File jasperFontsTempDir = new File(System.getProperty("java.io.tmpdir"), "jasper-fonts");

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            if (zipFile == null) {
                boolean exists = new File(jasperFontsTempDir, md5).exists();
                if (exists)
                    loadFontExtensions();
                return exists;
            } else {
                FileUtils.cleanDirectory(jasperFontsTempDir);
                File zip = new File(jasperFontsTempDir, md5);
                zipFile.getRawFile().write(zip);
                try(ZipFile zf = new ZipFile(zip)) {
                    zf.extractAll(jasperFontsTempDir.getAbsolutePath());
                }
                loadFontExtensions();
                return null;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void loadFontExtensions() throws IOException {
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jasperFontsTempDir.toURI().toURL()}, Thread.currentThread().getContextClassLoader()));
        JRPropertiesMap properties = JRPropertiesMap.loadProperties(new URL("file:" + jasperFontsTempDir.getAbsolutePath() + "/jasperreports_extension.properties"));
        new ReportExtensionsRegistryFactory().createRegistry("simple.font.families", properties).ensureFontExtensions();
    }
}