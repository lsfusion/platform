package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.server.logics.property.actions.file.FTPPath;
import lsfusion.server.logics.property.actions.file.Path;
import lsfusion.server.logics.property.actions.file.ReadUtils;
import lsfusion.server.logics.property.actions.file.WriteUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class FileUtils {

    public static void moveFile(String sourcePath, String destinationPath) throws SQLException, JSchException, SftpException, IOException {
        Path srcPath = Path.parsePath(sourcePath);
        Path destPath = Path.parsePath(destinationPath);

        if(srcPath.type.equals("file") && destPath.type.equals("file")) {
            rename(new File(srcPath.path), new File(destPath.path));
        } else {

            ReadUtils.ReadResult readResult = ReadUtils.readFile(sourcePath, false, false, false);
            if (readResult != null) {
                File sourceFile = new File(readResult.filePath);
                try {
                    switch (destPath.type) {
                        case "file":
                            FileCopyUtils.copy(sourceFile, new File(destPath.path));
                            break;
                        case "ftp":
                            WriteUtils.storeFileToFTP(destPath.path, sourceFile, null);
                            break;
                        case "sftp":
                            WriteUtils.storeFileToSFTP(destPath.path, sourceFile, null);
                            break;
                    }
                } finally {
                    deleteFile(readResult.filePath);
                    switch (srcPath.type) {
                        case "ftp":
                            deleteFTPFile(srcPath.path);
                            break;
                        case "sftp":
                            deleteSFTPFile(srcPath.path);
                            break;
                    }
                }
            }
        }
    }

    private static void rename(File srcFile, File destFile) {
        if(!srcFile.exists()) {
            throw new RuntimeException("Read Error. File not found: " + srcFile);
        } else if(!(srcFile.renameTo(destFile))) {
            throw new RuntimeException(String.format("Failed to move file from '%s' to '%s'", srcFile.getAbsolutePath(), destFile.getAbsolutePath()));
        }
    }

    public static void delete(String sourcePath) throws IOException, SftpException, JSchException {
        Path path = Path.parsePath(sourcePath);
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
                throw new RuntimeException(String.format("Failed to delete file '%s'", path), e);
            }
        } else {
            if (!sourceFile.exists() || !sourceFile.delete()) {
                throw new RuntimeException(String.format("Failed to delete file '%s'", path));
            }
        }
    }

    private static void deleteFTPFile(String path) throws IOException {
        FTPPath properties = FTPPath.parseFTPPath(path, 21);
        FTPClient ftpClient = new FTPClient();
        try {
            if (properties.charset != null) {
                ftpClient.setControlEncoding(properties.charset);
            }
            ftpClient.connect(properties.server, properties.port);
            ftpClient.login(properties.username, properties.password);
            if(properties.passiveMode) {
                ftpClient.enterLocalPassiveMode();
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean done = ftpClient.deleteFile(properties.remoteFile);
            if (!done) {
                throw new RuntimeException("Some error occurred while deleting file from ftp");
            }
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    private static void deleteSFTPFile(String path) throws JSchException, SftpException {
        FTPPath properties = FTPPath.parseFTPPath(path, 22);
        String remoteFile = properties.remoteFile;
        remoteFile = (!remoteFile.startsWith("/") ? "/" : "") + remoteFile;

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(properties.username, properties.server, properties.port);
            session.setPassword(properties.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            channelSftp.rm(remoteFile);
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    public static void mkdir(String directory) throws SftpException, JSchException, IOException {
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
                if (!mkdirSFTP(path.path)) {
                    result = "Failed to create directory '" + directory + "'";
                }
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

    private static String mkdirFTP(String path) throws IOException {
        String result = null;
        FTPPath ftpPath = FTPPath.parseFTPPath(path, 21);

        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
        if (ftpPath.charset != null)
            ftpClient.setControlEncoding(ftpPath.charset);
        try {

            ftpClient.connect(ftpPath.server, ftpPath.port);
            if (ftpClient.login(ftpPath.username, ftpPath.password)) {
                if(ftpPath.passiveMode) {
                    ftpClient.enterLocalPassiveMode();
                }
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                boolean dirExists = true;
                String[] directories = ftpPath.remoteFile.split("/");
                for (String dir : directories) {
                    if (result == null && !dir.isEmpty()) {
                        if (dirExists) dirExists = ftpClient.changeWorkingDirectory(dir);
                        if (!dirExists) {
                            if (!ftpClient.makeDirectory(dir)) result = ftpClient.getReplyString();
                            if (!ftpClient.changeWorkingDirectory(dir)) result = ftpClient.getReplyString();

                        }
                    }
                }

                return result;
            } else {
                result = "Incorrect login or password. Writing file from ftp failed";
            }
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static boolean mkdirSFTP(String path) throws JSchException, SftpException {
        FTPPath ftpPath = FTPPath.parseFTPPath(path, 22);

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ftpPath.username, ftpPath.server, ftpPath.port);
            session.setPassword(ftpPath.password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            if (ftpPath.charset != null)
                channelSftp.setFilenameEncoding(ftpPath.charset);
            channelSftp.mkdir(ftpPath.remoteFile.replace("\\", "/"));
            return true;
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    public static boolean checkFileExists(String sourcePath) throws IOException {
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
            default:
                throw new RuntimeException("FileExists is supported only for file and ftp");
        }
        return exists;
    }

    private static boolean checkFileExistsFTP(String path) throws IOException {
        FTPPath ftpPath = FTPPath.parseFTPPath(path, 21);
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setConnectTimeout(60000); //1 minute = 60 sec
            if(ftpPath.charset != null)
                ftpClient.setControlEncoding(ftpPath.charset);
            ftpClient.connect(ftpPath.server, ftpPath.port);
            ftpClient.login(ftpPath.username, ftpPath.password);
            if(ftpPath.passiveMode) {
                ftpClient.enterLocalPassiveMode();
            }

            InputStream inputStream = ftpClient.retrieveFileStream(ftpPath.remoteFile);
            int returnCode = ftpClient.getReplyCode();
            return inputStream != null && returnCode != 550;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    public static Map<String, Boolean> listFiles(String sourcePath, String charset) throws IOException {
        Path path = Path.parsePath(sourcePath);
        Map<String, Boolean> filesList;
        switch (path.type) {
            case "file":
                filesList = listFilesFile(path.path);
                break;
            case "ftp":
                filesList = listFilesFTP(path.path, charset);
                break;
            case "sftp":
            default:
                throw new RuntimeException("ListFiles supported only for file and ftp");
        }
        return filesList;
    }

    private static Map<String, Boolean> listFilesFile(String url) {
        TreeMap<String, Boolean> result = new TreeMap<>();

        File[] filesList = new File(url).listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                result.put(file.getName(), file.isDirectory());
            }
        } else {
            throw new RuntimeException(String.format("Path '%s' not found", url));
        }
        return result;
    }

    private static Map<String, Boolean> listFilesFTP(String path, String charset) throws IOException {
        FTPPath ftpPath = FTPPath.parseFTPPath(path, 21);
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setConnectTimeout(60000); //1 minute = 60 sec
            ftpClient.setControlEncoding(ftpPath.charset != null ? ftpPath.charset : charset);
            ftpClient.connect(ftpPath.server, ftpPath.port);
            ftpClient.login(ftpPath.username, ftpPath.password);
            if(ftpPath.passiveMode) {
                ftpClient.enterLocalPassiveMode();
            }

            Map<String, Boolean> result = new HashMap<>();
            if (ftpPath.remoteFile == null || ftpPath.remoteFile.isEmpty() || ftpClient.changeWorkingDirectory(ftpPath.remoteFile)) {
                FTPFile[] ftpFileList = ftpClient.listFiles();
                for (FTPFile file : ftpFileList) {
                    result.put(file.getName(), file.isDirectory());
                }
            } else {
                throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path));
            }
            return result;
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }
}