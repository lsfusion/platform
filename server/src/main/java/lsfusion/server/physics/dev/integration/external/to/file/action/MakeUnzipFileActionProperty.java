package lsfusion.server.physics.dev.integration.external.to.file.action;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MakeUnzipFileActionProperty extends ScriptingAction {

    public MakeUnzipFileActionProperty(UtilsLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            FileData unzippingFile = (FileData)findProperty("unzipping[]").read(context);
            if(unzippingFile != null) {
                RawFileData file = unzippingFile.getRawFile();
                String extension = unzippingFile.getExtension();

                Map<String, FileData> result = new HashMap<>();
                if (extension.toLowerCase().equals("rar")) {
                    result = unpackRARFile(file);
                } else if (extension.toLowerCase().equals("zip")) {
                    result = unpackZIPFile(file);
                }
                for(Map.Entry<String, FileData> entry : result.entrySet()) {
                    findProperty("unzipped[VARSTRING[100]]").change(entry.getValue(), context, new DataObject(entry.getKey()));
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

            List<File> dirList = new ArrayList<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if(inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                Archive a = new Archive(new FileVolumeManager(inputFile));

                FileHeader fh = a.nextFileHeader();

                while (fh != null) {
                    String fileName = (fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString());
                    outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                    File dir = outputFile.getParentFile();
                    dir.mkdirs();
                    if(!dirList.contains(dir))
                        dirList.add(dir);
                    if(!outputFile.isDirectory()) {
                        try (FileOutputStream os = new FileOutputStream(outputFile)) {
                            a.extractFile(fh, os);
                        }
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                        if(!outputFile.delete())
                            outputFile.deleteOnExit();
                    }
                    fh = a.nextFileHeader();
                }
                a.close();
            }

            for(File dir : dirList)
                if(dir != null && dir.exists() && !dir.delete())
                    dir.deleteOnExit();

        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if(inputFile != null && !inputFile.delete())
                inputFile.deleteOnExit();
            if(outputFile != null && !outputFile.delete())
                outputFile.deleteOnExit();
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
            Set<File> dirList = new HashSet<>();
            File outputDirectory = new File(inputFile.getParent() + "/" + getFileNameWithoutExt(inputFile));
            if(inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                dirList.add(outputDirectory);
                ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    if(ze.isDirectory()) {
                        File dir = new File(outputDirectory.getPath() + "/" + ze.getName());
                        dir.mkdirs();
                        dirList.add(dir);
                    }
                    else {
                        String fileName = ze.getName();
                        outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        int len;
                        while ((len = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.close();
                        result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                        if(!outputFile.delete())
                            outputFile.deleteOnExit();
                    }
                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }

            for(File dir : dirList)
                if(dir != null && dir.exists() && !dir.delete())
                    dir.deleteOnExit();

        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if(inputFile != null && !inputFile.delete())
                inputFile.deleteOnExit();
            if(outputFile != null && !outputFile.delete())
                outputFile.deleteOnExit();
        }
        return result;
    }

    private String getFileName(Map<String, FileData> files, String fileName) {
        if (files.containsKey(fileName)) {
            String name = getFileNameWithoutExt(fileName);
            String extension = BaseUtils.getFileExtension(fileName);
            int count = 1;
            while (files.containsKey(name + "_" + count + (extension.isEmpty() ? "" : ("." + extension)))) {
                count++;
            }
            fileName = name + "_" + count + (extension.isEmpty() ? "" : ("." + extension));
        }
        return fileName;
    }

    private String getFileNameWithoutExt(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        return (index == -1) ? name : name.substring(0, index);
    }

    private String getFileNameWithoutExt(String name) {
        int index = name.lastIndexOf(".");
        return (index == -1) ? name : name.substring(0, index);
    }
}