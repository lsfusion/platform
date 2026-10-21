package lsfusion.server.physics.dev.integration.external.to.file;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.FileUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static File makeZipFile(File[] files) throws IOException {
        File zipFile = File.createTempFile("zip", ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        String fileName = f.getName();
                        InputStream bis = Files.newInputStream(f.toPath());
                        zos.putNextEntry(new ZipEntry(fileName));
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = bis.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                        bis.close();
                    }
                }
            }
        }
        return zipFile;
    }

    public static Map<String, FileData> unpackFile(RawFileData file, boolean throwUnsupported) {
        if (FileUtils.isZip(file.getBytes())) {
            return unpackZIPFile(file);
        } else if (FileUtils.isRar(file.getBytes())) {
            return unpackRARFile(file);
        } else if (throwUnsupported) {
            throw new RuntimeException("File is not a zip or rar archive");
        } else {
            return new HashMap<>();
        }
    }

    private static Map<String, FileData> unpackZIPFile(RawFileData file) {
        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputDirectory = null;
        try {
            inputFile = File.createTempFile("zip", ".zip");
            file.write(inputFile);

            byte[] buffer = new byte[1024];
            outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(inputFile.toPath()), Charset.forName("cp866"));

                ZipEntry ze = inputStream.getNextEntry();
                while (ze != null) {
                    if (!ze.isDirectory()) {
                        String filePath = ze.getName();
                        File outputFile = new File(outputDirectory.getPath() + "/" + filePath);
                        if(isInsideDirectory(outputDirectory, outputFile)) { //ignore files that can be unpacked outside the temp dir
                            outputFile.getParentFile().mkdirs();
                            FileOutputStream outputStream = new FileOutputStream(outputFile);
                            int len;
                            while ((len = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                            }
                            outputStream.close();
                            result.put(getFileName(result, filePath), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                            BaseUtils.safeDelete(outputFile);
                        }
                    }
                    ze = inputStream.getNextEntry();
                }
                inputStream.closeEntry();
                inputStream.close();
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            org.apache.commons.io.FileUtils.deleteQuietly(outputDirectory);
        }
        return result;
    }

    private static Map<String, FileData> unpackRARFile(RawFileData file) {
        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputDirectory = null;
        try {
            inputFile = File.createTempFile("rar", ".rar");
            file.write(inputFile);
            outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                try (Archive a = new Archive(inputFile)) {
                    FileHeader fh = a.nextFileHeader();
                    while (fh != null) {
                        if (!fh.isDirectory()) {
                            String fileName = fh.getFileName().replace('\\', '/');
                            File outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                            if (isInsideDirectory(outputDirectory, outputFile)) { //ignore entries that can be unpacked outside the temp dir
                                outputFile.getParentFile().mkdirs();
                                try (FileOutputStream os = new FileOutputStream(outputFile)) {
                                    a.extractFile(fh, os);
                                }
                                result.put(getFileName(result, fileName), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                                BaseUtils.safeDelete(outputFile);
                            }
                        }
                        fh = a.nextFileHeader();
                    }
                }
            }
        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            org.apache.commons.io.FileUtils.deleteQuietly(outputDirectory);
        }
        return result;
    }

    private static boolean isInsideDirectory(File dir, File file) throws IOException {
        return file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
    }

    private static String getFileName(Map<String, FileData> files, String fileName) {
        if (files.containsKey(fileName)) {
            String extension = BaseUtils.getFileExtension(fileName);
            String name = extension.isEmpty() ? fileName : fileName.substring(0, fileName.length() - extension.length() - 1);
            int count = 1;
            while (files.containsKey(getFileName(name, count, extension))) {
                count++;
            }
            fileName = getFileName(name, count, extension);
        }
        return fileName;
    }

    private static String getFileName(String name, int count, String extension) {
        return name + "_" + count + (extension.isEmpty() ? "" : ("." + extension));
    }

}
