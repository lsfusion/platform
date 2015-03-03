package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadActionProperty extends ScriptingActionProperty {
    private final LCP<?> targetProp;

    public ReadActionProperty(ScriptingLogicsModule LM, ValueClass valueClass, LCP<?> targetProp) {
        super(LM, valueClass);
        this.targetProp = targetProp;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof StringClass;

        String path = (String) value.object;
        try {
            if (path != null) {
                Pattern p = Pattern.compile("(file|ftp|http|sql):\\/\\/(.*)");
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    File file = null;
                    if (type.equals("file")) {
                        file = new File(url);
                    } else if (type.equals("http")) {
                        file = File.createTempFile("downloaded", "tmp");
                        FileUtils.copyURLToFile(new URL(path), file);
                    } else if (type.equals("ftp")) {
                        file = File.createTempFile("downloaded", "tmp");
                        copyFTPToFile(path, file);
                    } else if (type.equals("sql")) {
                        file = File.createTempFile("downloaded", "tmp");
                        copySQLToFile(path, file);
                    }
                    if (file != null && file.exists()) {
                        targetProp.change(IOUtils.getFileBytes(file), context);
                        if (!type.equals("file"))
                            file.delete();
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

    private void copyFTPToFile(String path, File file) throws IOException {
        /*ftp://username:password@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.groupCount() == 4;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remoteFile = connectionStringMatcher.group(noPort ? 4 : 5);
            FTPClient ftpClient = new FTPClient();
            try {

                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                OutputStream outputStream = new FileOutputStream(file);
                boolean done = ftpClient.retrieveFile(remoteFile, outputStream);
                outputStream.close();
                if (!done) {
                    throw Throwables.propagate(new RuntimeException("Some error occurred while downloading file from ftp"));
                }
            } catch (FileNotFoundException e) {
                throw Throwables.propagate(e);
            } catch (SocketException e) {
                throw Throwables.propagate(e);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password@host:port/path_to_file"));
        }
    }

    private void copySQLToFile(String query, File file) throws SQLException {
        /*sql://username:password@host:port;dbName;query*/
        Pattern queryPattern = Pattern.compile("sql:\\/\\/(.*):(.*)@([^:]*)(?::(\\d*))?;([^;]*);(.*)");
        Matcher queryMatcher = queryPattern.matcher(query);
        if (queryMatcher.matches()) {
            Connection conn = null;

            try {
                String sqlUsername = queryMatcher.group(1);
                String sqlPassword = queryMatcher.group(2);
                String sqlHost = queryMatcher.group(3);
                Integer sqlPort = queryMatcher.group(4) == null ? 1433 : Integer.parseInt(queryMatcher.group(4));
                String sqlDBName = queryMatcher.group(5);
                String sqlQuery = queryMatcher.group(6);
                conn = getConnection(sqlHost, sqlPort, sqlDBName, sqlUsername, sqlPassword);

                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery(sqlQuery);

                    FileUtils.writeByteArrayToFile(file, BaseUtils.serializeResultSet(rs));

                } finally {
                    if (statement != null)
                        statement.close();
                }

            } catch (ClassNotFoundException e) {
                throw Throwables.propagate(e);
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (conn != null)
                    conn.close();
            }

        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect sql url. Please use format: sql://username:password@host:port;dbName;query"));
        }
    }

    protected Connection getConnection(String sqlHost, Integer sqlPort, String sqlDBName, String sqlUsername, String sqlPassword) throws ClassNotFoundException, SQLException {
        String url;
        //we specify instance OR port
        if (sqlHost != null && sqlHost.contains("\\")) {
            url = String.format("jdbc:sqlserver://%s;databaseName=%s;User=%s;Password=%s", sqlHost, sqlDBName, sqlUsername, sqlPassword);
        } else {
            url = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;User=%s;Password=%s", sqlHost, sqlPort, sqlDBName, sqlUsername, sqlPassword);
        }
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(url);
    }
}
