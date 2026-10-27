package lsfusion.server.physics.dev.integration.external.to.file;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.BaseBlock;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.rarfile.rar5.Rar5BaseBlock;
import com.github.junrar.rarfile.rar5.Rar5BlockType;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.FileUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.lambda.EConsumer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static FileData makeZipFile(Map<String, RawFileData> files, boolean zeroTime) throws IOException {
        Map<String, EConsumer<OutputStream, IOException>> writers = new LinkedHashMap<>();
        for (Map.Entry<String, RawFileData> file : files.entrySet())
            writers.put(file.getKey(), file.getValue()::write);
        return writeZipFile(writers, zeroTime);
    }

    public static FileData writeZipFile(Map<String, EConsumer<OutputStream, IOException>> files, boolean zeroTime) throws IOException {
        File zipFile = File.createTempFile("zip", ".zip");
        try {
            try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
                for (Map.Entry<String, EConsumer<OutputStream, IOException>> file : files.entrySet()) {
                    ZipEntry ze = new ZipEntry(file.getKey());
                    if(zeroTime)
                        ze.setTime(0); //to make zip file deterministic
                    zos.putNextEntry(ze);
                    file.getValue().accept(zos);
                }
            }
            return new FileData(new RawFileData(Files.readAllBytes(zipFile.toPath())), "zip");
        } finally {
            BaseUtils.safeDelete(zipFile);
        }
    }

    // zips the files into a temporary file, streaming them, since they can be bigger than the memory (backup files);
    // the caller deletes the result
    public static File makeZipFile(File[] files) throws IOException {
        File zipFile = File.createTempFile("zip", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            for (File file : files)
                if (file.isFile()) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    Files.copy(file.toPath(), zos);
                }
        } catch (IOException | RuntimeException e) {
            BaseUtils.safeDelete(zipFile);
            throw e;
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

            outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
            if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                try (ZipFile zipFile = new ZipFile(inputFile, Charset.forName("cp866"))) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry ze = entries.nextElement();
                        String filePath = ze.getName().replace('\\', '/');
                        if (!filePath.endsWith("/")) {
                            File outputFile = new File(outputDirectory.getPath() + "/" + filePath);
                            if (isInsideDirectory(outputDirectory, outputFile)) { //ignore files that can be unpacked outside the temp dir
                                outputFile.getParentFile().mkdirs();
                                try (InputStream inputStream = zipFile.getInputStream(ze)) {
                                    Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                                result.put(getFileName(result, filePath), new FileData(new RawFileData(outputFile), BaseUtils.getFileExtension(outputFile)));
                                BaseUtils.safeDelete(outputFile);
                            }
                        }
                    }
                }
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
                    if (a.hasBrokenHeaders() || isTruncatedRar5(a))
                        throw new RuntimeException("Unexpected end of archive");
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

    private static boolean isTruncatedRar5(Archive archive) {
        boolean rar5 = false;
        for (BaseBlock block : archive.getHeaders()) {
            if (block instanceof Rar5BaseBlock) {
                if (((Rar5BaseBlock) block).getRar5Type() == Rar5BlockType.ENDARC)
                    return false;
                rar5 = true;
            }
        }
        return rar5;
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
