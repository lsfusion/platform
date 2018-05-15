package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ReadUtils;
import lsfusion.server.logics.property.actions.WriteActionProperty;
import org.apache.commons.lang3.StringUtils;
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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static void moveFile(ExecutionContext context, String sourcePath, String destinationPath, boolean isClient) throws SQLException, JSchException, SftpException, IOException {
        Path srcPath = parsePath(sourcePath, isClient);
        Path destPath = parsePath(destinationPath, isClient);

        if(isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(2, srcPath.path, destPath.path));
            if (!result)
                throw new RuntimeException(String.format("Failed to move file from %s to %s", sourcePath, destinationPath));
        } else {
            ReadUtils.ReadResult readResult = ReadUtils.readFile(sourcePath, false, false, false);
            if (readResult.errorCode == 0) {
                File sourceFile = new File(readResult.filePath);
                try {
                    switch (destPath.type) {
                        case "ftp":
                            WriteActionProperty.storeFileToFTP(destinationPath, sourceFile);
                            break;
                        case "sftp":
                            WriteActionProperty.storeFileToSFTP(destinationPath, sourceFile);
                            break;
                        default:
                            ServerLoggers.importLogger.info(String.format("Writing file to %s", destinationPath));
                            FileCopyUtils.copy(sourceFile, new File(destPath.path));
                            break;
                    }
                } finally {
                    deleteFile(srcPath, sourcePath);
                }

            } else if (readResult.error != null) {
                throw new RuntimeException(readResult.error);
            }
        }
    }

    public static void deleteFile(ExecutionContext context, String sourcePath, boolean isClient) throws SftpException, JSchException, IOException {
        Path path = parsePath(sourcePath, isClient);
        if (isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(1, path.path));
            if (!result)
                throw new RuntimeException(String.format("Failed to delete file '%s'", sourcePath));
        } else {
            deleteFile(path, sourcePath);
        }
    }

    private static void deleteFile(Path path, String sourcePath) throws IOException, SftpException, JSchException {
        File sourceFile = new File(path.path);
        if (!sourceFile.delete()) {
            throw new RuntimeException(String.format("Failed to delete file '%s'", sourceFile));
        }
        if (path.type.equals("ftp")) {
            //todo: parseFTPPath может принимать путь без ftp://
            ReadUtils.deleteFTPFile(sourcePath);
        } else if (path.type.equals("sftp")) {
            //todo: parseFTPPath может принимать путь без ftp://
            ReadUtils.deleteSFTPFile(sourcePath);
        }
    }

    public static void mkdir(String directory) throws SftpException, JSchException, IOException {
        Path path = parsePath(directory, false);

        String result = null;
        switch (path.type) {
            case "file":
                if (!new File(path.path).exists() && !new File(path.path).mkdirs())
                    result = "Failed to create directory '" + directory + "'";
                break;
            case "ftp":
                result = mkdirFTP(path.path);
                break;
            case "sftp":
                if (!mkdirSFTP(path.path))
                    result = "Failed to create directory '" + directory + "'";
                break;
        }
        if (result != null)
            throw new RuntimeException(result);
    }

    private static String mkdirFTP(String path) throws IOException {
        String result = null;
        /*username:password;charset@host:port/directory*/
        Pattern connectionStringPattern = Pattern.compile("(.*):([^;]*)(?:;(.*))?@([^/:]*)(?::([^/]*))?(?:/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1);
            String password = connectionStringMatcher.group(2);
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4);
            boolean noPort = connectionStringMatcher.group(5) == null;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(5));
            String directory = connectionStringMatcher.group(6);

            FTPClient ftpClient = new FTPClient();
            ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
            if (charset != null)
                ftpClient.setControlEncoding(charset);
            try {

                ftpClient.connect(server, port);
                if (ftpClient.login(username, password)) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                    boolean dirExists = true;
                    String[] directories = directory.split("/");
                    for (String dir : directories) {
                        if (result == null && !dir.isEmpty()) {
                            if (dirExists)
                                dirExists = ftpClient.changeWorkingDirectory(dir);
                            if (!dirExists) {
                                if (!ftpClient.makeDirectory(dir))
                                    result = ftpClient.getReplyString();
                                if (!ftpClient.changeWorkingDirectory(dir))
                                    result = ftpClient.getReplyString();

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
        } else {
            result = "Incorrect ftp url. Please use format: ftp://username:password;charset@host:port/directory";
        }
        return result;
    }

    private static boolean mkdirSFTP(String path) throws JSchException, SftpException {
        /*username:password;charset@host:port/directory*/
        Pattern connectionStringPattern = Pattern.compile("(.*):([^;]*)(?:;(.*))?@([^/:]*)(?::([^/]*))?(?:/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //username
            String password = connectionStringMatcher.group(2); //password
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4); //host:IP
            boolean noPort = connectionStringMatcher.group(5) == null;
            Integer port = noPort ? 22 : Integer.parseInt(connectionStringMatcher.group(5));
            String directory = connectionStringMatcher.group(6);

            Session session = null;
            Channel channel = null;
            ChannelSftp channelSftp = null;
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(username, server, port);
                session.setPassword(password);
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                channel = session.openChannel("sftp");
                channel.connect();
                channelSftp = (ChannelSftp) channel;
                if (charset != null)
                    channelSftp.setFilenameEncoding(charset);
                channelSftp.mkdir(directory.replace("\\", "/"));
                return true;
            } finally {
                if (channelSftp != null)
                    channelSftp.exit();
                if (channel != null)
                    channel.disconnect();
                if (session != null)
                    session.disconnect();
            }
        } else {
            throw new RuntimeException("Incorrect sftp url. Please use format: sftp://username:password;charset@host:port/directory");
        }
    }

    public static boolean checkFileExists(ExecutionContext context, String sourcePath, String charset, boolean isClient) throws IOException {
        FileUtils.Path path = FileUtils.parsePath(sourcePath, isClient);
        boolean exists;
        if (isClient) {
            exists = (boolean) context.requestUserInteraction(new FileClientAction(0, path.path));
        } else {
            switch (path.type) {
                case "ftp":
                    exists = checkFileExistsFTP(path.path, charset);
                    break;
                case "sftp":
                    throw new RuntimeException("FileExists for SFTP is not supported");
                case "file":
                default:
                    exists = new File(path.path).exists();
                    break;
            }
        }
        return exists;
    }

    private static boolean checkFileExistsFTP(String path, String charset) throws IOException {
        //*username:password@host:port/path*//*
        Pattern connectionStringPattern = Pattern.compile("(.*):(.*)@([^/:]*)(?::([^/]*))?(?:/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1);
            String password = connectionStringMatcher.group(2);
            String server = connectionStringMatcher.group(3);
            boolean noPort = connectionStringMatcher.groupCount() == 4;
            Integer port = noPort || connectionStringMatcher.group(4) == null ? 21 : Integer.parseInt(connectionStringMatcher.group(4));
            String remotePath = connectionStringMatcher.group(noPort ? 4 : 5);
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.setConnectTimeout(60000); //1 minute = 60 sec
                ftpClient.setControlEncoding(charset);
                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();

                InputStream inputStream = ftpClient.retrieveFileStream(remotePath);
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
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password@host:port/path"));
        }
    }

    public static Map<String, Boolean> listFiles(ExecutionContext context, String sourcePath, String charset, boolean isClient) throws IOException {
        FileUtils.Path path = FileUtils.parsePath(sourcePath, isClient);
        Map<String, Boolean> filesList;
        if (isClient) {
            filesList = (Map<String, Boolean>) context.requestUserInteraction(new FileClientAction(3, path.path));
        } else {
            switch (path.type) {
                case "ftp":
                    filesList = getFilesListFTP(context, path.path, charset);
                    break;
                case "sftp":
                    throw new RuntimeException("ListFiles for SFTP is not supported");
                case "file":
                default:
                    filesList = getFilesList(path.path);
                    break;
            }
        }
        return filesList;
    }

    private static Map<String, Boolean> getFilesList(String url) {
        TreeMap<String, Boolean> result = new TreeMap<>();

        File[] filesList = new File(url).listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                result.put(file.getName(), file.isDirectory());
            }
        }
        return result;
    }

    private static Map<String, Boolean> getFilesListFTP(ExecutionContext context, String path, String charset) throws IOException {
        //*username:password@host:port/path*//*
        Pattern connectionStringPattern = Pattern.compile("(.*):(.*)@([^/:]*)(?::([^/]*))?(?:/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.groupCount() == 4;
            Integer port = noPort || connectionStringMatcher.group(4) == null ? 21 : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remotePath = connectionStringMatcher.group(noPort ? 4 : 5);
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.setConnectTimeout(60000); //1 minute = 60 sec
                ftpClient.setControlEncoding(charset);
                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();

                Map<String, Boolean> result = new HashMap<>();
                if (ftpClient.changeWorkingDirectory(remotePath)) {
                    FTPFile[] ftpFileList = ftpClient.listFiles();
                    for (FTPFile file : ftpFileList) {
                        result.put(file.getName(), file.isDirectory());
                    }
                } else {
                    context.delayUserInteraction(new MessageClientAction(String.format("Path '%s' not found for %s", remotePath, path), "Path not found"));
                }
                return result;

            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password@host:port/path"));
        }
    }

    private static Path parsePath(String sourcePath,  boolean isClient) {
        String pattern = isClient ? "(file):(?://)?(.*)" : "(file|ftp|sftp):(?://)?(.*)";
        String[] types = isClient ? new String[]{"file:"} : new String[]{"file:", "ftp:", "sftp:"};

        if (!StringUtils.startsWithAny(sourcePath, types)) {
            sourcePath = "file:" + sourcePath;
        }
        Matcher m = Pattern.compile(pattern).matcher(sourcePath);
        if (m.matches()) {
            return new Path(m.group(1).toLowerCase(), m.group(2));
        } else {
            throw Throwables.propagate(new RuntimeException("Unsupported path: " + sourcePath));
        }
    }

    private static class Path {
        public String type;
        public String path;

        public Path(String type, String path) {
            this.type = type;
            this.path = path;
        }
    }
}