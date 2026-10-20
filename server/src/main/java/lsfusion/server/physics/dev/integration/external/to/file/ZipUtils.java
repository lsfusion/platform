package lsfusion.server.physics.dev.integration.external.to.file;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            return unpackRARFile(file, throwUnsupported);
        } else if (throwUnsupported) {
            throw new RuntimeException("File is not a zip or rar archive");
        } else {
            return new HashMap<>();
        }
    }

    private static Map<String, FileData> unpackZIPFile(RawFileData file) {
        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("zip", ".zip");
            file.write(inputFile);

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
                        if (isInsideDirectory(outputDirectory, dir) && dir.mkdirs())
                            dirSet.add(dir);
                    } else {
                        String filePath = ze.getName();
                        if(isInsideDirectory(outputDirectory, new File(outputDirectory.getPath() + "/" + filePath))) { //ignore files that can be unpacked outside the temp dir
                            String[] splitted = filePath.split("/");

                            StringBuilder path = new StringBuilder();
                            for (int i = 0; i < splitted.length - 1; i++) {
                                path.append("/").append(splitted[i]);
                                File dir = new File(outputDirectory.getPath() + path);
                                if (dir.mkdirs())
                                    dirSet.add(dir);
                            }

                            outputFile = new File(outputDirectory.getPath() + "/" + filePath);
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

    private static Map<String, FileData> unpackRARFile(RawFileData file, boolean throwUnsupported) {
        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("rar", ".rar");
            file.write(inputFile);
            boolean isRar5 = checkRar5(file, throwUnsupported);
            if (!isRar5) {
                Set<File> dirSet = new HashSet<>();
                File outputDirectory = new File(inputFile.getParent() + "/" + BaseUtils.getFileName(inputFile));
                if (inputFile.exists() && (outputDirectory.exists() || outputDirectory.mkdir())) {
                    dirSet.add(outputDirectory);
                    Archive a = new Archive(new FileVolumeManager(inputFile));
                    FileHeader fh = a.nextFileHeader();
                    while (fh != null) {
                        String fileName = (fh.isUnicode() ? fh.getFileNameW() : fh.getFileNameString());
                        outputFile = new File(outputDirectory.getPath() + "/" + fileName);
                        if (isInsideDirectory(outputDirectory, outputFile)) { //ignore entries that can be unpacked outside the temp dir
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
                        }
                        fh = a.nextFileHeader();
                    }
                    a.close();
                }

                for (File dir : dirSet) {
                    BaseUtils.safeDelete(dir);
                }
            }
        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            BaseUtils.safeDelete(outputFile);
        }
        return result;
    }

    private static boolean checkRar5(RawFileData file, boolean throwUnsupported) {
        boolean isRar5 = FileUtils.isRar5(file.getBytes());
        if (isRar5 && throwUnsupported) {
            throw new RuntimeException("RAR5 archive is not supported");
        }
        return isRar5;
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
