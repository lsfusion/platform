package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontExtensionsCollector;
import net.sf.jasperreports.engine.fonts.SimpleFontExtensionHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CopyReportResourcesClientAction extends ExecuteClientAction {
    public final FileData zipFile;
    public final String md5;

    public CopyReportResourcesClientAction(FileData zipFile, String md5) {
        this.zipFile = zipFile;
        this.md5 = md5;
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

            File zip = new File(jasperFontsTempDir, md5);
            zipFile.getRawFile().write(zip);

            for (String font : getFonts(zip)) {
                fontExtensionHelper.loadFontExtensions(context, font, extensionsCollector);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private List<String> getFonts(File zip) {
        List<String> result = new ArrayList<>();
        try {
            byte[] buffer = new byte[1024];
            if (zip.exists()) {
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(zip.toPath()), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    String filePath = ze.getName();

                    String[] splitted = filePath.split("/");
                    if (splitted.length > 1) {
                        String path = "";
                        for (int i = 0; i < splitted.length - 1; i++) {
                            path += "/" + splitted[i];
                            new File(jasperFontsTempDir.getPath() + path).mkdir();
                        }
                    }
                    FileOutputStream outputStream = new FileOutputStream(new File(jasperFontsTempDir.getPath() + "/" + filePath));
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.close();

                    if(filePath.endsWith(".xml"))
                        result.add(jasperFontsTempDir + "/" + filePath);

                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return result;
    }
}