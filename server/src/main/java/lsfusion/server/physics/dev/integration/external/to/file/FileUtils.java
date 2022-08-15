package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.*;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static lsfusion.base.DateConverter.sqlTimestampToLocalDateTime;
import static lsfusion.base.file.WriteUtils.appendExtension;

public class FileUtils {

    public static void moveFile(String sourcePath, String destinationPath) throws SQLException, IOException {
        copyFile(sourcePath, destinationPath, true);
    }

    public static void copyFile(String sourcePath, String destinationPath) throws SQLException, IOException {
        copyFile(sourcePath, destinationPath, false);
    }

    private static void copyFile(String sourcePath, String destinationPath, boolean move) throws IOException, SQLException {
        Path srcPath = Path.parsePath(sourcePath);
        Path destPath = Path.parsePath(destinationPath);

        if (srcPath.type.equals("file") && destPath.type.equals("file")) {
            copyFile(new File(srcPath.path), new File(destPath.path), move);
        } else if (move && equalFTPServers(srcPath, destPath)) {
            renameFTP(srcPath.path, destPath.path, null);
        } else {
            ReadUtils.ReadResult readResult = ReadUtils.readFile(sourcePath, false, false, false, null);
            if (readResult != null) {
                RawFileData rawFile = (RawFileData) readResult.fileBytes;
                switch (destPath.type) {
                    case "file":
                        rawFile.write(destPath.path);
                        break;
                    case "ftp":
                        WriteUtils.storeFileToFTP(destPath.path, rawFile, null);
                        break;
                    case "sftp":
                        WriteUtils.storeFileToSFTP(destPath.path, rawFile, null);
                        break;
                }
                if (move) {
                    delete(srcPath);
                }
            }
        }
    }

    private static void copyFile(File srcFile, File destFile, boolean move) throws IOException {
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' is a directory");
        }
        if(move) {
            boolean rename = srcFile.renameTo(destFile);
            if (!rename) {
                org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
                if (!srcFile.delete()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(destFile);
                    throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
                }
            }
        } else {
            org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
        }
    }

    private static boolean equalFTPServers(Path srcPath, Path destPath) {
        if (srcPath.type.equals("ftp") && destPath.type.equals("ftp")) {
            FTPPath srcProperties = FTPPath.parseFTPPath(srcPath.path);
            FTPPath destProperties = FTPPath.parseFTPPath(destPath.path);
            return srcProperties.server.equals(destProperties.server) && srcProperties.port.equals(destProperties.port);
        } else return false;
    }

    public static void renameFTP(String srcPath, String destPath, String extension) {
        IOUtils.ftpAction(srcPath, (srcProperties, ftpClient) -> {
            try {
                FTPPath destProperties = FTPPath.parseFTPPath(destPath);
                boolean done = ftpClient.rename(appendExtension(srcProperties.remoteFile, extension), appendExtension(destProperties.remoteFile, extension));
                if (!done) {
                    throw new RuntimeException("Failed to rename ftp file: " + ftpClient.getReplyString());
                }
                return null;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    public static void delete(String sourcePath) {
        delete(Path.parsePath(sourcePath));
    }

    public static void delete(Path path) {
        switch (path.type) {
            case "file":
                deleteFile(path.path);
                break;
            case "ftp":
                deleteFTPFile(path.path);
                break;
            case "sftp":
                deleteSFTPFile(path.path);
                break;
        }
    }

    private static void deleteFile(String path) {
        File sourceFile = new File(path);
        if(sourceFile.isDirectory()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(sourceFile);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        } else {
            try {
                Files.delete(sourceFile.toPath());
            } catch (IOException e) {
                throw ExceptionUtils.propagateWithMessage(e, "Absolute path: " + sourceFile.getAbsolutePath());
            }
        }
    }

    private static void deleteFTPFile(String path) {
        IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                boolean done = ftpClient.deleteFile(ftpPath.remoteFile);
                if (!done) {
                    throw new RuntimeException("Failed to delete '" + path + "'");
                }
                return null;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static void deleteSFTPFile(String path) {
        IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                if(ftpPath.remoteFile.endsWith("/")) {
                    channelSftp.rmdir(ftpPath.remoteFile);
                } else {
                    channelSftp.rm(ftpPath.remoteFile);
                }
                return null;
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), e);
                else
                    throw Throwables.propagate(e);
            }
        });
    }

    public static void mkdir(String directory) {
        Path path = Path.parsePath(directory);
        String result = null;
        switch (path.type) {
            case "file":
                result = mkdirFile(path.path, directory);
                break;
            case "ftp":
                result = mkdirFTP(path.path);
                break;
            case "sftp":
                result = mkdirSFTP(path.path);
                break;
        }
        if (result != null) {
            throw new RuntimeException(result);
        }
    }

    private static String mkdirFile(String path, String directory) {
        String result = null;
        File file = new File(path);
        if (!file.exists() && !file.mkdirs()) {
            result = "Failed to create directory '" + directory + "'";
        }
        return result;
    }

    private static String mkdirFTP(String path) {
        return IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                String result = null;
                boolean dirExists = true;
                String[] directories = ftpPath.remoteFile.split("/");
                for (String dir : directories) {
                    if (result == null && !dir.isEmpty()) {
                        if (dirExists) dirExists = ftpClient.changeWorkingDirectory(dir);
                        if (!dirExists) {
                            if (!ftpClient.makeDirectory(dir)) {
                                result = ftpClient.getReplyString();
                            }
                            if (!ftpClient.changeWorkingDirectory(dir)) {
                                result = ftpClient.getReplyString();
                            }

                        }
                    }
                }
                return result;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static String mkdirSFTP(String path) {
        return IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                channelSftp.mkdir(ftpPath.remoteFile);
                return null;
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    return String.format("Path '%s' not found for %s", ftpPath.remoteFile, path);
                else
                    return String.format("Failed to create directory '%s' (%s)", path, e.getMessage());
            }
        });
    }

    public static boolean checkFileExists(String sourcePath) {
        Path path = Path.parsePath(sourcePath);
        boolean exists;
        switch (path.type) {
            case "file":
                exists = new File(path.path).exists();
                break;
            case "ftp":
                exists = checkFileExistsFTP(path.path);
                break;
            case "sftp":
                exists = checkFileExistsSFTP(path.path);
                break;
            default:
                throw new RuntimeException("FileExists unsupported type " + path.type);
        }
        return exists;
    }

    private static boolean checkFileExistsFTP(String path) {
        return IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                boolean exists;
                //check file existence
                try (InputStream inputStream = ftpClient.retrieveFileStream(ftpPath.remoteFile)) {
                    exists = inputStream != null && ftpClient.getReplyCode() != 550;
                }
                if (!exists) {
                    //check directory existence
                    exists = ftpClient.changeWorkingDirectory(ftpPath.remoteFile);
                }
                return exists;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static boolean checkFileExistsSFTP(String path) {
        return IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                channelSftp.ls(ftpPath.remoteFile); //list files throws exception if file/directory not found
                return true;
            } catch (SftpException exception) {
                if(exception.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    return false;
                else
                    throw Throwables.propagate(exception);
            }
        });
    }

    public static List<Object> listFiles(String sourcePath, boolean recursive) throws IOException {
        Path path = Path.parsePath(sourcePath);
        List<Object> filesList;
        switch (path.type) {
            case "file":
                filesList = listFilesFile(path.path, recursive);
                break;
            case "ftp":
                if(recursive)
                    throw new RuntimeException("ListFiles recursive is not supported for FTP");
                filesList = listFilesFTP(path.path);
                break;
            case "sftp":
                if(recursive)
                    throw new RuntimeException("ListFiles recursive is not supported for SFTP");
                filesList = listFilesSFTP(path.path);
                break;
            default:
                throw new RuntimeException("ListFiles unsupported type " + path.type);
        }
        return filesList;
    }

    private static List<Object> listFilesFile(String url, boolean recursive) throws IOException {
        List<Object> result;

        java.nio.file.Path urlPath = Paths.get(url);
        if (Files.exists(urlPath)) {
            try (Stream<java.nio.file.Path> pathStream = (recursive ? Files.walk(urlPath) : Files.list(urlPath)).filter(path -> !path.equals(urlPath))) {
                List<java.nio.file.Path> pathList = pathStream.collect(Collectors.toList());
                String[] nameValues = new String[pathList.size()];
                Boolean[] isDirectoryValues = new Boolean[pathList.size()];
                LocalDateTime[] modifiedDateTimeValues = new LocalDateTime[pathList.size()];
                for (int i = 0; i < pathList.size(); i++) {
                    File file = pathList.get(i).toFile();
                    nameValues[i] = urlPath.relativize(pathList.get(i)).toFile().getPath();
                    isDirectoryValues[i] = file.isDirectory() ? true : null;
                    modifiedDateTimeValues[i] = sqlTimestampToLocalDateTime(new Timestamp(file.lastModified()));
                }
                result = Arrays.asList(nameValues, isDirectoryValues, modifiedDateTimeValues);
            }
        } else {
            throw new RuntimeException(String.format("Path '%s' not found", url));
        }
        return result;
    }

    private static List<Object> listFilesFTP(String path) {
        return IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                if (ftpPath.remoteFile == null || ftpPath.remoteFile.isEmpty() || ftpClient.changeWorkingDirectory(ftpPath.remoteFile)) {
                    FTPFile[] ftpFileList = ftpClient.listFiles();
                    String[] nameValues = new String[ftpFileList.length];
                    Boolean[] isDirectoryValues = new Boolean[ftpFileList.length];
                    LocalDateTime[] modifiedDateTimeValues = new LocalDateTime[ftpFileList.length];
                    for (int i = 0; i < ftpFileList.length; i++) {
                        FTPFile file = ftpFileList[i];
                        nameValues[i] = file.getName();
                        isDirectoryValues[i] = file.isDirectory() ? true : null;
                        modifiedDateTimeValues[i] = sqlTimestampToLocalDateTime(new Timestamp(file.getTimestamp().getTimeInMillis()));
                    }
                    return Arrays.asList(nameValues, isDirectoryValues, modifiedDateTimeValues);
                } else {
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path));
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static List<Object> listFilesSFTP(String path) {
        return IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                Vector result = channelSftp.ls(ftpPath.remoteFile); //list files throws exception if file/directory not found
                List<String> nameValues = new ArrayList<>();
                List<Boolean> isDirectoryValues = new ArrayList<>();
                List<LocalDateTime> modifiedDateTimeValues = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    ChannelSftp.LsEntry file = (ChannelSftp.LsEntry) result.elementAt(i);
                    String fileName = file.getFilename();
                    if (!fileName.equals(".") && !fileName.equals("..")) {
                        nameValues.add(file.getFilename());
                        SftpATTRS attrs = file.getAttrs();
                        isDirectoryValues.add(attrs.isDir() ? true : null);
                        modifiedDateTimeValues.add(sqlTimestampToLocalDateTime(new Timestamp(attrs.getMTime() * 1000L)));
                    }
                }
                //noinspection RedundantCast
                return Arrays.asList((Object) nameValues.toArray(new String[0]), isDirectoryValues.toArray(new Boolean[0]), modifiedDateTimeValues.toArray(new LocalDateTime[0]));
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), e);
                else
                    throw Throwables.propagate(e);
            }
        });
    }

    public static void safeDelete(File file) {
        if (file != null && !file.delete()) {
            file.deleteOnExit();
        }
    }

    public static String runCmd(String command, String directory) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process p = directory != null ? runtime.exec(command, null, new File(directory)) : runtime.exec(command);
        try (BufferedInputStream err = new BufferedInputStream(p.getErrorStream())) {
            StringBuilder errS = new StringBuilder();
            byte[] b = new byte[1024];
            while (err.read(b) != -1) {
                errS.append(new String(b, SystemUtils.IS_OS_WINDOWS ?  "cp866" : "utf-8").trim()).append("\n");
            }
            return BaseUtils.trimToNull(errS.toString());
        }
    }
}