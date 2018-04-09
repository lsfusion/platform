package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.SaveFileClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteActionProperty extends SystemExplicitActionProperty {
    private final Type sourcePropertyType;
    private boolean clientAction;
    private boolean dialog;
    private boolean append;

    public WriteActionProperty(Type sourcePropertyType, boolean clientAction, boolean dialog, boolean append, ValueClass sourceProp, ValueClass pathProp) {
        super(pathProp == null ? new ValueClass[]{sourceProp} : new ValueClass[]{sourceProp, pathProp});
        this.sourcePropertyType = sourcePropertyType;
        this.clientAction = clientAction;
        this.dialog = dialog;
        this.append = append;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sourceObject = context.getDataKeys().getValue(0);
        assert sourceObject.getType() instanceof FileClass;
        byte[] fileBytes = (byte[]) sourceObject.object;

        String path = null;
        if(context.getDataKeys().size() == 2) {
            DataObject pathObject = context.getDataKeys().getValue(1);
            assert pathObject.getType() instanceof StringClass;
            path = (String) pathObject.object;
        }

        String extension = null;
        if (fileBytes != null) {
            if (sourcePropertyType instanceof StaticFormatFileClass) {
                extension = ((StaticFormatFileClass) sourcePropertyType).getOpenExtension(fileBytes);
            } else if (sourcePropertyType instanceof DynamicFormatFileClass) {
                extension = BaseUtils.getExtension(fileBytes);
                fileBytes = BaseUtils.getFile(fileBytes);
            }
        }
        try {
            if (fileBytes != null) {
                if (clientAction) {
                    path = path == null || path.isEmpty() ? "file" : appendExtension(path, extension);
                    if (path.contains("/") || path.contains("\\")) {
                        processClientAbsolutePath(context, fileBytes, path);
                    } else {
                        processClientRelativePath(context, fileBytes, path);
                    }
                } else {
                    processServerAbsolutePath(fileBytes, path, extension);
                }
            } else {
                throw new RuntimeException("File bytes not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void processClientAbsolutePath(ExecutionContext context, byte[] fileBytes, String path) {
        Pattern p = Pattern.compile("(file://)?(.*)");
        Matcher m = p.matcher(path);
        if (m.matches()) {
            path = m.group(2);
        }
        context.delayUserInterfaction(new SaveFileClientAction(fileBytes, path, !dialog, append));
    }

    private void processClientRelativePath(ExecutionContext context, byte[] fileBytes, String path) {
        String filePath = dialog ? path : (System.getProperty("user.home") + "/Downloads/" + path);
        context.delayUserInterfaction(new SaveFileClientAction(fileBytes, filePath, !dialog, append));
    }

    private void processServerAbsolutePath(byte[] fileBytes, String path, String extension) throws IOException, SftpException, JSchException {
        if (path != null && !path.isEmpty()) {
            path = appendExtension(path, extension);
            Pattern p = Pattern.compile("(?:(file|ftp|sftp)://)?(.*)");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                String type = m.group(1) == null ? "file" : m.group(1).toLowerCase();
                String url = m.group(2);

                switch (type) {
                    case "file": {
                        File file = new File(url);
                        if (!file.getParentFile().exists())
                            throw new RuntimeException(String.format("Path is incorrect or not found: %s", url));
                        else
                            writeFile(file.getAbsolutePath(), fileBytes, append);
                        break;
                    }
                    case "ftp": {
                        if(append)
                            throw new RuntimeException("APPEND is not supported in WRITE to FTP");
                        File file = null;
                        try {
                            file = File.createTempFile("downloaded", ".tmp");
                            IOUtils.putFileBytes(file, fileBytes);
                            storeFileToFTP(path, file);
                        } finally {
                            if (file != null && !file.delete())
                                file.deleteOnExit();
                        }
                        break;
                    }
                    case "sftp": {
                        if(append)
                            throw new RuntimeException("APPEND is not supported in WRITE to SFTP");
                        File file = null;
                        try {
                            file = File.createTempFile("downloaded", ".tmp");
                            IOUtils.putFileBytes(file, fileBytes);
                            storeFileToSFTP(path, file);
                        } finally {
                            if (file != null && !file.delete())
                                file.deleteOnExit();
                        }
                        break;
                    }
                }
            } else {
                throw new RuntimeException("Incorrect path. Please use format: file://path_to_file or ftp|sftp://username:password;charset@host:port/path_to_file");
            }
        }
    }

    private String appendExtension(String path, String extension) {
        return BaseUtils.getFileExtension(path).isEmpty() && extension != null && !extension.isEmpty() ? (path + "." + extension) : path;
    }

    private static void writeFile(String filePath, byte[] fileBytes, boolean append) throws IOException {
        if (append) {
            String extension = BaseUtils.getFileExtension(filePath);
            if (extension.equals("csv")) {
                if (new File(filePath).exists())
                    Files.write(Paths.get(filePath), fileBytes, StandardOpenOption.APPEND);
                else
                    IOUtils.putFileBytes(new File(filePath), fileBytes);
            } else {
                throw new RuntimeException("APPEND is supported only for csv files");
            }
        } else {
            IOUtils.putFileBytes(new File(filePath), fileBytes);
        }
    }

    public static void storeFileToFTP(String path, File file) throws IOException {
        ServerLoggers.importLogger.info(String.format("Writing file to %s", path));
        /*ftp://username:password;charset@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):([^;]*)(?:;(.*))?@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.group(5) == null;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(5)); //21
            String remoteFile = connectionStringMatcher.group(6);

            FTPClient ftpClient = new FTPClient();
            ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
            if (charset != null)
                ftpClient.setControlEncoding(charset);
            try {

                ftpClient.connect(server, port);
                if (ftpClient.login(username, password)) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

                    InputStream inputStream = new FileInputStream(file);
                    boolean done = ftpClient.storeFile(remoteFile, inputStream);
                    inputStream.close();
                    if (done)
                        ServerLoggers.importLogger.info(String.format("Successful writing file to %s", path));
                    else {
                        ServerLoggers.importLogger.error(String.format("Failed writing file to %s", path));
                        throw new RuntimeException("Some error occurred while writing file to ftp");
                    }
                } else {
                    throw new RuntimeException("Incorrect login or password. Writing file from ftp failed");
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
            throw new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password;charset@host:port/path_to_file");
        }
    }

    public static void storeFileToSFTP(String path, File file) throws JSchException, SftpException, FileNotFoundException {
        /*sftp://username:password;charset@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("sftp:\\/\\/(.*):([^;]*)(?:;(.*))?@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //username
            String password = connectionStringMatcher.group(2); //password
            String charset = connectionStringMatcher.group(3);
            String server = connectionStringMatcher.group(4); //host:IP
            boolean noPort = connectionStringMatcher.group(5) == null;
            Integer port = noPort ? 22 : Integer.parseInt(connectionStringMatcher.group(5));
            String filePath = connectionStringMatcher.group(6);
            File remoteFile = new File((!filePath.startsWith("/") ? "/" : "") + filePath);

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
                channelSftp.cd(remoteFile.getParent().replace("\\", "/"));
                channelSftp.put(new FileInputStream(file), remoteFile.getName());
            } finally {
                if (channelSftp != null)
                    channelSftp.exit();
                if (channel != null)
                    channel.disconnect();
                if (session != null)
                    session.disconnect();
            }
        } else {
            throw new RuntimeException("Incorrect sftp url. Please use format: sftp://username:password;charset@host:port/path_to_file");
        }
    }
}
