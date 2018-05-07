package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileExistsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface charsetInterface;
    private final ClassPropertyInterface isClientInterface;

    public FileExistsActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        charsetInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String path = (String) context.getKeyValue(pathInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        try {
            context.getSession().dropChanges((DataProperty) findProperty("fileExists[]").property);
            if (path != null) {
                if (isClient) {
                    Pattern p = Pattern.compile("file:(?://)?(.*)");
                    Matcher m = p.matcher(path);
                    if (m.matches()) {
                        String url = m.group(1);
                        boolean exists = (boolean) context.requestUserInteraction(new FileClientAction(0, url));
                        findProperty("fileExists[]").change(exists ? true : null, context);
                    } else
                        throw new RuntimeException("ListFiles Error. Incorrect path: " + path);
                } else {
                    Pattern p = Pattern.compile("(file|ftp):(?://)?(.*)");
                    Matcher m = p.matcher(path);
                    if (m.matches()) {
                        String type = m.group(1).toLowerCase();
                        String url = m.group(2);

                        boolean exists = false;
                        if (type.equals("file")) {
                            exists = new File(url).exists();
                        } else if (type.equals("ftp")) {
                            exists = checkFileExistsFTP(path, charset);
                        }
                        findProperty("fileExists[]").change(exists ? true : null, context);
                    } else {
                        throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. Incorrect path: " + path));
                    }

                }
            } else {
                throw new RuntimeException("ListFiles Error. Path not specified.");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean checkFileExistsFTP(String path, String charset) throws IOException {
        //*ftp://username:password@host:port/path*//*
        Pattern connectionStringPattern = Pattern.compile("ftp://(.*):(.*)@([^/:]*)(?::([^/]*))?(?:/(.*))?");
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

    @Override
    protected boolean allowNulls() {
        return true;
    }
}