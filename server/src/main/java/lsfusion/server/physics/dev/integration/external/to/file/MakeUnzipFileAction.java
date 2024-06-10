package lsfusion.server.physics.dev.integration.external.to.file;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MakeUnzipFileAction extends InternalAction {

    public MakeUnzipFileAction(UtilsLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            FileData unzippingFile = (FileData) findProperty("unzipping[]").read(context);
            if (unzippingFile != null) {
                RawFileData file = unzippingFile.getRawFile();
                String extension = unzippingFile.getExtension();

                Map<String, FileData> result = new HashMap<>();
                if (extension.equalsIgnoreCase("rar")) {
                    result = unpackRARFile(file);
                } else if (extension.equalsIgnoreCase("zip")) {
                    result = unpackZIPFile(file);
                }
                for (Map.Entry<String, FileData> entry : result.entrySet()) {
                    findProperty("unzipped[STRING[100]]").change(entry.getValue(), context, new DataObject(entry.getKey()));
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private Map<String, FileData> unpackRARFile(RawFileData fileBytes) {

        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".rar");
            fileBytes.write(inputFile);

            Set<File> dirSet = new HashSet<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirSet.add(outputDirectory);
                Archive a = new Archive(new FileVolumeManager(inputFile));

                FileHeader fh = a.nextFileHeader();

                while (fh != null) {
                    String fileName = (fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString());
                    outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                    File dir = outputFile.getParentFile();
                    if (dir.mkdirs())
                        dirSet.add(dir);
                    if (!outputFile.isDirectory()) {
                        try (FileOutputStream os = new FileOutputStream(outputFile)) {
                            a.extractFile(fh, os);
                        }
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                        BaseUtils.safeDelete(outputFile);
                    }
                    fh = a.nextFileHeader();
                }
                a.close();
            }

            for (File dir : dirSet) {
                BaseUtils.safeDelete(dir);
            }

        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            BaseUtils.safeDelete(outputFile);
        }
        return result;
    }

    private Map<String, FileData> unpackZIPFile(RawFileData fileBytes) {

        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("email", ".zip");
            fileBytes.write(inputFile);

            byte[] buffer = new byte[1024];
            Set<File> dirSet = new HashSet<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirSet.add(outputDirectory);
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(inputFile.toPath()), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    if (ze.isDirectory()) {
                        File dir = new File(outputDirectory.getPath() + "/" + ze.getName());
                        if (dir.mkdirs())
                            dirSet.add(dir);
                    } else {
                        String filePath = ze.getName();

                        String[] splitted = filePath.split("/");

                        String fileName;
                        if (splitted.length > 1) {
                            String path = "";
                            for (int i = 0; i < splitted.length - 1; i++) {
                                path += "/" + splitted[i];
                                File dir = new File(outputDirectory.getPath() + path);
                                if (dir.mkdirs())
                                    dirSet.add(dir);
                            }
                            fileName = splitted[splitted.length - 1];
                        } else {
                            fileName = filePath;
                        }

                        outputFile = new File(outputDirectory.getPath() + "/" + filePath);
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        int len;
                        while ((len = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.close();
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                        BaseUtils.safeDelete(outputFile);
                    }
                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }

            for (File dir : dirSet) {
                BaseUtils.safeDelete(dir);
            }

        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            BaseUtils.safeDelete(outputFile);
        }
        return result;
    }

    private String getFileName(Map<String, FileData> files, String fileName) {
        if (files.containsKey(fileName)) {
            String name = BaseUtils.getFileName(fileName);
            String extension = BaseUtils.getFileExtension(fileName);
            int count = 1;
            while (files.containsKey(name + "_" + count + (extension.isEmpty() ? "" : ("." + extension)))) {
                count++;
            }
            fileName = name + "_" + count + (extension.isEmpty() ? "" : ("." + extension));
        }
        return fileName;
    }
}