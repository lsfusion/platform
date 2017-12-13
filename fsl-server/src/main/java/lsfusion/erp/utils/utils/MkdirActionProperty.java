package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MkdirActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface directoryInterface;

    public MkdirActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingModuleErrorLog.SemanticError {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        directoryInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String directory = (String) context.getKeyValue(directoryInterface).getValue();
        if (directory != null && !directory.isEmpty()) {
            try {
                Pattern p = Pattern.compile("(file|ftp|sftp):\\/\\/(.*)");
                Matcher m = p.matcher(directory);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    String result = null;
                    switch (type) {
                        case "file": {
                            if (!new File(url).exists() && !new File(url).mkdirs())
                                result = "Failed to create directory '" + directory + "'";
                            break;
                        }
                        case "ftp": {
                            result = mkdirFTP(directory);
                            break;
                        }
                        case "sftp": {
                            if (!mkdirSFTP(directory))
                                result = "Failed to create directory '" + directory + "'";
                            break;
                        }
                    }
                    if (result != null)
                        context.delayUserInterfaction(new MessageClientAction(result, "Error"));
                } else {
                    throw Throwables.propagate(new RuntimeException("Incorrect path. Please use format: file://directory or ftp|sftp://username:password;charset@host:port/directory"));
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Path not specified"));
        }
    }

    private String mkdirFTP(String path) throws IOException {
        String result = null;
        /*ftp://username:password;charset@host:port/directory*/
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):([^;]*)(?:;(.*))?@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
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

    private boolean mkdirSFTP(String path) throws JSchException, SftpException, FileNotFoundException {
        /*sftp://username:password;charset@host:port/directory*/
        Pattern connectionStringPattern = Pattern.compile("sftp:\\/\\/(.*):([^;]*)(?:;(.*))?@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
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
            throw Throwables.propagate(new RuntimeException("Incorrect sftp url. Please use format: sftp://username:password;charset@host:port/directory"));
        }
    }
}