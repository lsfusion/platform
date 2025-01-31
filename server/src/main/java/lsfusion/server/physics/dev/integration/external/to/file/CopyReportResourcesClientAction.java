package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontExtensionsCollector;
import net.sf.jasperreports.engine.fonts.SimpleFontExtensionHelper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

public class CopyReportResourcesClientAction extends ExecuteClientAction {
    public final Map<String, RawFileData> files;

    public CopyReportResourcesClientAction(Map<String, RawFileData> files) {
        this.files = files;
    }

    public final File jasperFontsTempDir = new File(System.getProperty("java.io.tmpdir"), "jasper-fonts");

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        try {

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jasperFontsTempDir.toURI().toURL()}, originalClassLoader));
            SimpleFontExtensionHelper fontExtensionHelper = SimpleFontExtensionHelper.getInstance();
            DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
            FontExtensionsCollector extensionsCollector = new FontExtensionsCollector();

            for (Map.Entry<String, RawFileData> file : files.entrySet()) {
                String filePath = file.getKey();
                File f = new File(jasperFontsTempDir, filePath);
                file.getValue().write(f);
                if (filePath.endsWith(".xml")) {
                    fontExtensionHelper.loadFontExtensions(context, filePath, extensionsCollector);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}