package lsfusion.server.physics.dev.integration.external.to.file;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    public static Map<String, FileData> unpackFile(RawFileData file, String extension, boolean throwUnsupported) {
        Map<String, FileData> result;
        if (extension.equalsIgnoreCase("zip")) {
            result = unpackZIPFile(file);
        } else if (extension.equalsIgnoreCase("rar")) {
            result = unpackRARFile(file, throwUnsupported);
        } else {
            result = new HashMap<>();
        }
        return result;
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
                        if (dir.mkdirs())
                            dirSet.add(dir);
                    } else {
                        String filePath = ze.getName();

                        String[] splitted = filePath.split("/");

                        String fileName;
                        if (splitted.length > 1) {
                            StringBuilder path = new StringBuilder();
                            for (int i = 0; i < splitted.length - 1; i++) {
                                path.append("/").append(splitted[i]);
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

    private static Map<String, FileData> unpackRARFile(RawFileData file, boolean throwUnsupported) {
        Map<String, FileData> result = new HashMap<>();
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("rar", ".rar");
            file.write(inputFile);
            boolean isRar5 = checkRar5(inputFile, throwUnsupported);
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
            }
        } catch (RarException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            BaseUtils.safeDelete(inputFile);
            BaseUtils.safeDelete(outputFile);
        }
        return result;
    }

    private static boolean checkRar5(File file, boolean throwUnsupported) throws IOException {
        boolean isRar5 = false;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            if (fis.read(header) != 8) {
                throw new RuntimeException("File too short to be a RAR archive");
            }
            if (header[0] == 'R' && header[1] == 'a' && header[2] == 'r' && header[3] == '!' &&
                    header[4] == 0x1A && header[5] == 0x07 && header[6] == 0x01) { //0x00 - rar4, 0x01 - rar5
                isRar5 = true;
                if(throwUnsupported) {
                    throw new RuntimeException("RAR5 archive is not supported");
                }
            }
        }
        return isRar5;
    }

    private static String getFileName(Map<String, FileData> files, String fileName) {
        if (files.containsKey(fileName)) {
            String name = BaseUtils.getFileName(fileName);
            String extension = BaseUtils.getFileExtension(fileName);
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
