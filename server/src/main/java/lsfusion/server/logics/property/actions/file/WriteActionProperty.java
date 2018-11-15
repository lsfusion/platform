package lsfusion.server.logics.property.actions.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.FileData;
import lsfusion.base.RawFileData;
import lsfusion.interop.action.SaveFileClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.utils.WriteUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
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

        String path = null;
        if(context.getDataKeys().size() == 2) {
            DataObject pathObject = context.getDataKeys().getValue(1);
            assert pathObject.getType() instanceof StringClass;
            path = (String) pathObject.object;
        }

        String extension = null;
        RawFileData rawFileData = null;
        if (sourceObject.object != null) {
            if (sourcePropertyType instanceof StaticFormatFileClass) {
                rawFileData = (RawFileData) sourceObject.object;
                extension = ((StaticFormatFileClass) sourcePropertyType).getOpenExtension(rawFileData);
            } else if (sourcePropertyType instanceof DynamicFormatFileClass) {
                extension = ((FileData) sourceObject.object).getExtension();
                rawFileData = ((FileData) sourceObject.object).getRawFile();
            }
        }
        try {
            if (rawFileData != null) {
                if (clientAction) {
                    path = path == null || path.isEmpty() ? "file" : appendExtension(path, extension);
                    if (path.contains("/") || path.contains("\\")) {
                        processClientAbsolutePath(context, rawFileData, path);
                    } else {
                        processClientRelativePath(context, rawFileData, path);
                    }
                } else {
                    processServerAbsolutePath(rawFileData, path, extension);
                }
            } else {
                throw new RuntimeException("File bytes not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void processClientAbsolutePath(ExecutionContext context, RawFileData fileData, String path) {
        Pattern p = Pattern.compile("(file://)?(.*)");
        Matcher m = p.matcher(path);
        if (m.matches()) {
            path = m.group(2);
        }
        context.delayUserInterfaction(new SaveFileClientAction(fileData, path, !dialog, append));
    }

    private void processClientRelativePath(ExecutionContext context, RawFileData fileData, String path) {
        String filePath = dialog ? path : (System.getProperty("user.home") + "/Downloads/" + path);
        context.delayUserInterfaction(new SaveFileClientAction(fileData, filePath, !dialog, append));
    }

    private void processServerAbsolutePath(RawFileData fileData, String path, String extension) throws IOException, SftpException, JSchException {
        if (path != null && !path.isEmpty()) {
            Pattern p = Pattern.compile("(?:(file|ftp|sftp)://)?(.*)");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                String type = m.group(1) == null ? "file" : m.group(1).toLowerCase();
                String url = m.group(2);

                switch (type) {
                    case "file": {
                        //надо учесть, что путь может быть с точкой
                        if (extension != null && !extension.isEmpty()) {
                            url += "." + extension;
                        }
                        //url = appendExtension(url, extension);
                        File file = new File(url);
                        if (!file.getParentFile().exists())
                            throw new RuntimeException(String.format("Path is incorrect or not found: %s", url));
                        else
                            writeFile(file.getAbsolutePath(), fileData, append);
                        break;
                    }
                    case "ftp": {
                        if(append)
                            throw new RuntimeException("APPEND is not supported in WRITE to FTP");
                        File file = null;
                        try {
                            //для ftp и sftp пока оставляем старую схему - добавляем расширение всегда
                            if (extension != null && !extension.isEmpty()) {
                                path += "." + extension;
                            }
                            file = File.createTempFile("downloaded", ".tmp");
                            fileData.write(file);
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
                            if (extension != null && !extension.isEmpty()) {
                                path += "." + extension;
                            }
                            file = File.createTempFile("downloaded", ".tmp");
                            fileData.write(file);
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

    private static void writeFile(String filePath, RawFileData fileData, boolean append) throws IOException {
        if (append) {
            String extension = BaseUtils.getFileExtension(filePath);
            switch (extension) {
                case "csv":
                    if (new File(filePath).exists()) {
                        fileData.append(filePath);
                    } else {
                        fileData.write(filePath);
                    }
                    break;
                case "xls": {
                    File file = new File(filePath);
                    if (file.exists()) {
                        HSSFWorkbook sourceWB = new HSSFWorkbook(fileData.getInputStream());
                        HSSFWorkbook destinationWB = new HSSFWorkbook(new FileInputStream(file));
                        WriteUtils.copyHSSFSheets(sourceWB, destinationWB);

                        try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                            destinationWB.write(fos);
                        }

                    } else {
                        fileData.write(filePath);
                    }
                    break;
                }
                case "xlsx":
                    File file = new File(filePath);
                    if (file.exists()) {
                        XSSFWorkbook sourceWB = new XSSFWorkbook(fileData.getInputStream());
                        XSSFWorkbook destinationWB = new XSSFWorkbook(new FileInputStream(file));
                        WriteUtils.copyXSSFSheets(sourceWB, destinationWB);

                        try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                            destinationWB.write(fos);
                        }

                    } else {
                        fileData.write(filePath);
                    }
                    break;
                default:
                    throw new RuntimeException("APPEND is supported only for csv, xls, xlsx files");
            }
        } else {
            fileData.write(filePath);
        }
    }

    public static void storeFileToFTP(String path, File file) throws IOException {
        ServerLoggers.importLogger.info(String.format("Writing file to %s", path));
        ReadUtils.FTPPath properties = ReadUtils.parseFTPPath(path, 21);
        if (properties != null) {
            FTPClient ftpClient = new FTPClient();
            ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
            if (properties.charset != null)
                ftpClient.setControlEncoding(properties.charset);
            try {

                ftpClient.connect(properties.server, properties.port);
                if (ftpClient.login(properties.username, properties.password)) {
                    if(properties.passiveMode) {
                        ftpClient.enterLocalPassiveMode();
                    }
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

                    InputStream inputStream = new FileInputStream(file);
                    boolean done = ftpClient.storeFile(properties.remoteFile, inputStream);
                    inputStream.close();
                    if (done)
                        ServerLoggers.importLogger.info(String.format("Successful writing file to %s", path));
                    else {
                        ServerLoggers.importLogger.error(String.format("Failed writing file to %s : " + ftpClient.getReplyCode(), path));
                        throw new RuntimeException("Error occurred while writing file to ftp : " + ftpClient.getReplyCode());
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
            throw new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password;charset@host:port/path_to_file?passivemode=false");
        }
    }

    public static void storeFileToSFTP(String path, File file) throws JSchException, SftpException, FileNotFoundException {
        /*sftp://username:password;charset@host:port/path_to_file*/
        ReadUtils.FTPPath properties = ReadUtils.parseFTPPath(path, 22);
        if (properties != null) {
            File remoteFile = new File((!properties.remoteFile.startsWith("/") ? "/" : "") + properties.remoteFile);

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
                if (properties.charset != null)
                    channelSftp.setFilenameEncoding(properties.charset);
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
