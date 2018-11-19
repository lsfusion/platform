package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.base.RawFileData;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.file.ReadUtils;
import lsfusion.server.logics.property.actions.file.WriteActionProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
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
                String error = null;
                try {
                    switch (destPath.type) {
                        case "ftp":
                            WriteActionProperty.storeFileToFTP(destinationPath, (RawFileData) readResult.fileBytes, null);
                            break;
                        case "sftp":
                            WriteActionProperty.storeFileToSFTP(destinationPath, (RawFileData) readResult.fileBytes, null);
                            break;
                        default:
                            ServerLoggers.importLogger.info(String.format("Writing file to %s", destinationPath));
                            FileCopyUtils.copy(new File(readResult.filePath), new File(destPath.path));
                            break;
                    }
                } finally {
                    switch (srcPath.type) {
                        case "ftp":
                            deleteFTPFile(srcPath.path);
                            break;
                        case "sftp":
                            deleteSFTPFile(srcPath.path);
                            break;
                        default:
                            if (!new File(readResult.filePath).delete()) {
                                error = String.format("Failed to delete file '%s'", readResult.filePath);
                            }
                            break;
                    }
                }
                if(error != null)
                    throw new RuntimeException(error);
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
            switch (path.type) {
                case "ftp":
                    deleteFTPFile(path.path);
                    break;
                case "sftp":
                    deleteSFTPFile(path.path);
                    break;
                case "file":
                    if (!new File(path.path).delete()) {
                        throw new RuntimeException(String.format("Failed to delete file '%s'", path.path));
                    }
                    break;
            }
        }
    }

    private static void deleteFTPFile(String path) throws IOException {
        FTPPath properties = parseFTPPath(path, 21);
        FTPClient ftpClient = new FTPClient();
        try {
            if (properties.charset != null)
                ftpClient.setControlEncoding(properties.charset);
            ftpClient.connect(properties.server, properties.port);
            ftpClient.login(properties.username, properties.password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean done = ftpClient.deleteFile(properties.remoteFile);
            if (!done) {
                throw new RuntimeException("Some error occurred while deleting file from ftp");
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    private static void deleteSFTPFile(String path) throws JSchException, SftpException {
        FTPPath properties = parseFTPPath(path, 22);
        String remoteFile = properties.remoteFile;
        remoteFile = (!remoteFile.startsWith("/") ? "/" : "") + remoteFile;

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(properties.username, properties.server, properties.port);
            session.setPassword(properties.password);
            java.util.Properties config = new java.util.Properties();
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

    public static void mkdir(ExecutionContext context, String directory, boolean isClient) throws SftpException, JSchException, IOException {
        Path path = parsePath(directory, isClient);
        if (isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(4, path.path));
            if (!result) {
                throw new RuntimeException(String.format("Failed to create directory '%s'", directory));
            }
        } else {
            String result = null;
            switch (path.type) {
                case "file":
                    File file = new File(path.path);
                    if (!file.exists() && !file.mkdirs()) {
                        result = "Failed to create directory '" + directory + "'";
                    }
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
    }

    private static String mkdirFTP(String path) throws IOException {
        String result = null;
        FTPPath ftpPath = parseFTPPath(path, 21);

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
        FTPPath ftpPath = parseFTPPath(path, 22);

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

    public static boolean checkFileExists(ExecutionContext context, String sourcePath, boolean isClient) throws IOException {
        Path path = parsePath(sourcePath, isClient);
        boolean exists;
        if (isClient) {
            exists = (boolean) context.requestUserInteraction(new FileClientAction(0, path.path));
        } else {
            switch (path.type) {
                case "ftp":
                    exists = checkFileExistsFTP(path.path);
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

    private static boolean checkFileExistsFTP(String path) throws IOException {
        FTPPath ftpPath = parseFTPPath(path, 21);
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

    public static Map<String, Boolean> listFiles(ExecutionContext context, String sourcePath, String charset, boolean isClient) throws IOException {
        Path path = parsePath(sourcePath, isClient);
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
        FTPPath ftpPath = parseFTPPath(path, 21);
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
                context.delayUserInteraction(new MessageClientAction(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), "Path not found"));
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
            throw new RuntimeException("Unsupported path: " + sourcePath);
        }
    }

    private static class Path {
        public String type;
        public String path;

        public Path(String type, String path) {
            this.type = type;
            this.path = path;
        }

        public String getPath() {
            return type + "://" + path;
        }
    }

    private static FTPPath parseFTPPath(String path, Integer defaultPort) {
        /*username:password;charset@host:port/path_to_file?passivemode=false*/
        Pattern connectionStringPattern = Pattern.compile("(.*):([^;]*)(?:;(.*))?@([^/:]*)(?::([^/]+))?(?:/([^?]*))?(?:\\?(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1);
            String password = connectionStringMatcher.group(2);
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4);
            Integer port = connectionStringMatcher.group(5) == null ? defaultPort : Integer.parseInt(connectionStringMatcher.group(5));
            String remoteFile = connectionStringMatcher.group(6);
            List<NameValuePair> extraParams = URLEncodedUtils.parse(connectionStringMatcher.group(7), charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8);
            boolean passiveMode = isPassiveMode(extraParams);
            return new FTPPath(username, password, charset, server, port, remoteFile, passiveMode);
        } else {
            throw new RuntimeException("Incorrect ftp url. Please use format: ftp(s)://username:password;charset@host:port/path_to_file?passivemode=false");
        }
    }

    private static boolean isPassiveMode(List<NameValuePair> queryParams) {
        String result = getParameterValue(queryParams, "passivemode");
        return result == null || result.equals("true");
    }

    private static String getParameterValue(List<NameValuePair> queryParams, String key) {
        List<String> values = new ArrayList<>();
        for(NameValuePair queryParam : queryParams) {
            if(queryParam.getName().equalsIgnoreCase(key))
                values.add(queryParam.getValue());
        }
        return values.isEmpty() ? null : values.get(0);
    }

    private static class FTPPath {
        String username;
        String password;
        String charset;
        String server;
        Integer port;
        String remoteFile;
        boolean passiveMode;

        public FTPPath(String username, String password, String charset, String server, Integer port, String remoteFile, boolean passiveMode) {
            this.username = username;
            this.password = password;
            this.charset = charset;
            this.server = server;
            this.port = port;
            this.remoteFile = remoteFile;
            this.passiveMode = passiveMode;
        }
    }
}