package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListFilesActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;

    public ListFilesActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String path = (String) context.getDataKeyValue(pathInterface).object;

        try {
            if (path != null) {
                Pattern p = Pattern.compile("(file|ftp):(?:\\/\\/)?(.*)");
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    Map<String, Boolean> filesList = new HashMap<>();
                    if (type.equals("file")) {
                        filesList = getFilesList(url);
                    } else if (type.equals("ftp")) {
                        filesList = getFTPFilesList(path);
                    }
                    if (filesList != null/* && !filesList.isEmpty()*/) {

                        Integer i = 0;
                        for (Map.Entry<String, Boolean> file : filesList.entrySet()) {
                            findProperty("fileName[INTEGER]").change(file.getKey(), context, new DataObject(i));
                            findProperty("fileIsDirectory[INTEGER]").change(file.getValue(), context, new DataObject(i));
                            i++;
                        }
                        Integer prevCount = (Integer) findProperty("prevCountFiles[]").read(context);
                        if (prevCount != null) {
                            while (i < prevCount) {
                                findProperty("fileName[INTEGER]").change((Object) null, context, new DataObject(i));
                                findProperty("fileIsDirectory[INTEGER]").change((Object) null, context, new DataObject(i));
                                i++;
                            }
                        }
                        findProperty("prevCountFiles[]").change(filesList.size(), context);
                    } else {
                        throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. File not found: " + path));
                    }
                } else {
                    throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. Incorrect path: " + path));
                }
            } else {
                throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. Path not specified."));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }


    private Map<String, Boolean> getFilesList(String url) throws IOException {
        TreeMap<String, Boolean> result = new TreeMap<>();

        File[] filesList = new File(url).listFiles();
        if(filesList != null) {
            for (File file : filesList) {
                result.put(file.getName(), file.isDirectory());
            }
        }
        return result;
    }

    private Map<String, Boolean> getFTPFilesList(String path) throws IOException {
        //*ftp://username:password@host:port/path*//*
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.groupCount() == 4;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remotePath = connectionStringMatcher.group(noPort ? 4 : 5);
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.setConnectTimeout(3600000); //1 minute = 60 sec
                ftpClient.setControlEncoding("UTF-8");
                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();

                Map<String, Boolean> result = new HashMap<>();
                FTPFile[] ftpFileList = ftpClient.listFiles(remotePath);
                for (FTPFile file : ftpFileList) {
                    result.put(file.getName(), file.isDirectory());
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
}
