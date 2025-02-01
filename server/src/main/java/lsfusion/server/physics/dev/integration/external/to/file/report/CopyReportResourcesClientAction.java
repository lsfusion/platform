package lsfusion.server.physics.dev.integration.external.to.file.report;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontExtensionsCollector;
import net.sf.jasperreports.engine.fonts.SimpleFontExtensionHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
                File zip = new File(jasperFontsTempDir, md5);
                zipFile.getRawFile().write(zip);
                unpackZip(zip);
                loadFontExtensions();
                return null;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void unpackZip(File zip) {
        try {
            byte[] buffer = new byte[1024];
            if (zip.exists()) {
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(zip.toPath()));

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
                    FileOutputStream outputStream = new FileOutputStream(jasperFontsTempDir.getPath() + "/" + filePath);
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.close();

                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void loadFontExtensions() throws IOException {
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jasperFontsTempDir.toURI().toURL()}, Thread.currentThread().getContextClassLoader()));
        SimpleFontExtensionHelper fontExtensionHelper = SimpleFontExtensionHelper.getInstance();
        DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
        FontExtensionsCollector extensionsCollector = new FontExtensionsCollector();

        java.nio.file.Path urlPath = Paths.get(jasperFontsTempDir.getPath());
        if (Files.exists(urlPath)) {
            try (Stream<Path> pathStream = (Files.walk(urlPath))) {
                for (Path path : pathStream.filter(path -> path.toString().endsWith(".xml")).collect(Collectors.toList())) {
                    fontExtensionHelper.loadFontExtensions(context, path.toString(), extensionsCollector);
                }
            }
        }
    }
}