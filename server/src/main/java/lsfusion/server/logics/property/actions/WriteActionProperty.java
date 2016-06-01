package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteActionProperty extends ScriptingActionProperty {
    private final LCP<?> sourceProp;
    
    public WriteActionProperty(ScriptingLogicsModule LM, ValueClass valueClass, LCP<?> sourceProp) {
        super(LM, valueClass);
        this.sourceProp = sourceProp;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof StringClass;

        String path = (String) value.object;
        byte[] fileBytes = (byte[]) sourceProp.read(context);
        String extension = null;
        Type sourcePropertyType = sourceProp.property.getType();
        if (fileBytes != null) {
            if (sourcePropertyType instanceof StaticFormatFileClass) {
                extension = ((StaticFormatFileClass) sourcePropertyType).getOpenExtension(fileBytes);
            } else if (sourcePropertyType instanceof DynamicFormatFileClass) {
                extension = BaseUtils.getExtension(fileBytes);
                fileBytes = BaseUtils.getFile(fileBytes);
            }
        }
        try {
            if (path != null && !path.isEmpty()) {
                if (fileBytes != null) {
                    if (extension != null && !extension.isEmpty()) {
                        path += "." + extension;    
                    }
                    Pattern p = Pattern.compile("(file|ftp|sftp):\\/\\/(.*)");
                    Matcher m = p.matcher(path);
                    if (m.matches()) {
                        String type = m.group(1).toLowerCase();
                        String url = m.group(2);

                        if (type.equals("file")) {
                            File file = new File(url);
                            if (!file.getParentFile().exists())
                                throw Throwables.propagate(new RuntimeException(String.format("Path is incorrect or not found: %s", url)));
                            else
                                IOUtils.putFileBytes(file, fileBytes);
                        } else if (type.equals("ftp")) {
                            File file = null;
                            try {
                                file = File.createTempFile("downloaded", ".tmp");
                                IOUtils.putFileBytes(file, fileBytes);
                                storeFileToFTP(path, file);
                            } finally {
                                if(file != null && !file.delete())
                                    file.deleteOnExit();
                            }
                        } else if (type.equals("sftp")) {
                            File file = null;
                            try {
                                file = File.createTempFile("downloaded", ".tmp");
                                IOUtils.putFileBytes(file, fileBytes);
                                storeFileToSFTP(path, file);
                            } finally {
                                if (file != null && !file.delete())
                                    file.deleteOnExit();
                            }
                        }
                    } else {
                        throw Throwables.propagate(new RuntimeException("Incorrect path. Please use format: file://path_to_file or ftp|sftp://username:password@host:port/path_to_file"));
                    }
                } else {
                    throw Throwables.propagate(new RuntimeException("File bytes not specified"));
                }
            } else {
                throw Throwables.propagate(new RuntimeException("Path not specified"));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void storeFileToFTP(String path, File file) throws IOException {
        ServerLoggers.importLogger.info(String.format("Writing file to %s", path));
        /*ftp://username:password@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.group(4) == null;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remoteFile = connectionStringMatcher.group(5);
            FTPClient ftpClient = new FTPClient();
            ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
            try {

                ftpClient.connect(server, port);
                if (ftpClient.login(username, password)) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                    InputStream inputStream = new FileInputStream(file);
                    boolean done = ftpClient.storeFile(remoteFile, inputStream);
                    inputStream.close();
                    if(done)
                        ServerLoggers.importLogger.info(String.format("Successful writing file to %s", path));
                    else {
                        ServerLoggers.importLogger.error(String.format("Failed writing file to %s", path));
                        throw Throwables.propagate(new RuntimeException("Some error occurred while writing file to ftp"));
                    }
                } else {
                    throw Throwables.propagate(new RuntimeException("Incorrect login or password. Writing file from ftp failed"));
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
            throw Throwables.propagate(new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password@host:port/path_to_file"));
        }
    }

    public static void storeFileToSFTP(String path, File file) throws JSchException, SftpException, FileNotFoundException {
        /*sftp://username:password@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("sftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //username
            String password = connectionStringMatcher.group(2); //password
            String server = connectionStringMatcher.group(3); //host:IP
            boolean noPort = connectionStringMatcher.group(4) == null;
            Integer port = noPort ? 22 : Integer.parseInt(connectionStringMatcher.group(4));
            String filePath = connectionStringMatcher.group(5);
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
                channelSftp.cd(remoteFile.getParent().replace("\\", "/"));
                channelSftp.put(new FileInputStream(file), remoteFile.getName());
            } finally {
                if(channelSftp != null)
                    channelSftp.exit();
                if(channel != null)
                    channel.disconnect();
                if(session != null)
                    session.disconnect();
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect sftp url. Please use format: sftp://username:password@host:port/path_to_file"));
        }
    }
}
