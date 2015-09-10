package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.jcraft.jsch.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.DynamicFormatFileClass;
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
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadActionProperty extends ScriptingActionProperty {
    private final LCP<?> targetProp;
    private final boolean delete;

    public ReadActionProperty(ScriptingLogicsModule LM, ValueClass sourceProp, LCP<?> targetProp, ValueClass moveProp, boolean delete) {
        super(LM, moveProp == null ? new ValueClass[] {sourceProp} : new ValueClass[] {sourceProp, moveProp});
        this.targetProp = targetProp;
        this.delete = delete;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sourceProp = context.getDataKeys().getValue(0);
        assert sourceProp.getType() instanceof StringClass;
        String sourcePath = (String) sourceProp.object;

        String movePath = null;
        if(context.getDataKeys().size() == 2) {
            DataObject moveProp = context.getDataKeys().getValue(1);
            assert moveProp.getType() instanceof StringClass;
            movePath = (String) moveProp.object;
        }
        boolean move = movePath != null;
        int errorCode = 0;
        String type = null;
        File file = null;

        try {
            if (sourcePath != null) {
                Pattern p = Pattern.compile("(file|ftp||sftp|http|jdbc|mdb):(?:\\/\\/)?(.*)");
                Matcher m = p.matcher(sourcePath);
                if (m.matches()) {
                    type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    String extension = null;
                    switch (type) {
                        case "file":
                            file = new File(url);
                            extension = BaseUtils.getFileExtension(file);
                            break;
                        case "http":
                            file = File.createTempFile("downloaded", "tmp");
                            FileUtils.copyURLToFile(new URL(sourcePath), file);
                            extension = BaseUtils.getFileExtension(new File(url));
                            break;
                        case "ftp":
                            file = File.createTempFile("downloaded", "tmp");
                            copyFTPToFile(sourcePath, file);
                            extension = BaseUtils.getFileExtension(new File(url));
                            break;
                        case "sftp":
                            file = File.createTempFile("downloaded", "tmp");
                            copySFTPToFile(sourcePath, file);
                            extension = BaseUtils.getFileExtension(new File(url));
                            break;
                        case "jdbc":
                            file = File.createTempFile("downloaded", "tmp");
                            extension = "jdbc";
                            copyJDBCToFile(sourcePath, file);
                            break;
                        case "mdb":
                            file = File.createTempFile("downloaded", "tmp");
                            copyMDBToFile(sourcePath, file);
                            extension = "mdb";
                            break;
                    }
                    if (file != null && file.exists()) {
                        if (targetProp.property.getType() instanceof DynamicFormatFileClass) {
                            targetProp.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(file), extension.getBytes()), context);
                        } else {
                            targetProp.change(IOUtils.getFileBytes(file), context);
                        }
                    } else {
                        errorCode = 3;
                    }
                } else {
                    errorCode = 2;
                }
            } else {
                errorCode = 1;
            }

            switch (errorCode) {
                case 0:
                    if(move) {
                        switch (type) {
                            case "file":
                                if (movePath.startsWith("file://"))
                                    FileCopyUtils.copy(file, new File(movePath.replace("file://", "")));
                                else
                                    throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. Unsupported movePath: " + movePath +
                                            ", supports only file://filepath"));
                                break;
                            case "ftp":
                                WriteActionProperty.storeFileToFTP(movePath, file);
                                break;
                            case "sftp":
                                WriteActionProperty.storeFileToSFTP(movePath, file);
                                break;
                        }
                    }
                    if (!type.equals("file") || delete || move)
                        if(!file.delete())
                            file.deleteOnExit();
                    if(delete || move) {
                        if (type.equals("ftp")) {
                            deleteFTPFile(sourcePath);
                        } else if (type.equals("sftp")) {
                            deleteSFTPFile(sourcePath);
                        }
                    }
                    break;
                case 1:
                    throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. Path not specified."));
                case 2:
                    throw Throwables.propagate(new RuntimeException(String.format("ReadActionProperty Error. Incorrect path: %s, use syntax (file|ftp|http|jdbc|mdb)://path", sourcePath)));
                case 3:
                    throw Throwables.propagate(new RuntimeException("ReadActionProperty Error. File not found: " + sourcePath));

            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void copyFTPToFile(String path, File file) throws IOException {
        List<Object> properties = parseFTPPath(path, 21);
        if (properties != null) {
            String username = (String) properties.get(0);
            String password = (String) properties.get(1);
            String server = (String) properties.get(2);
            Integer port = (Integer) properties.get(3);
            String remoteFile = (String) properties.get(4);
            FTPClient ftpClient = new FTPClient();
            ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
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

    private void copySFTPToFile(String path, File file) throws IOException, JSchException, SftpException {
        List<Object> properties = parseFTPPath(path, 22);
        if (properties != null) {
            String username = (String) properties.get(0);
            String password = (String) properties.get(1);
            String server = (String) properties.get(2);
            Integer port = (Integer) properties.get(3);
            String remoteFile = (String) properties.get(4);
            remoteFile = (!remoteFile.startsWith("/") ? "/" : "") + remoteFile;

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
                channelSftp.get(remoteFile, file.getAbsolutePath());
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

    private void deleteFTPFile(String path) throws IOException {
        List<Object> properties = parseFTPPath(path, 21);
        if (properties != null) {
            String username = (String) properties.get(0);
            String password = (String) properties.get(1);
            String server = (String) properties.get(2);
            Integer port = (Integer) properties.get(3);
            String remoteFile = (String) properties.get(4);
            FTPClient ftpClient = new FTPClient();
            try {

                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                boolean done = ftpClient.deleteFile(remoteFile);
                if (!done) {
                    throw Throwables.propagate(new RuntimeException("Some error occurred while deleting file from ftp"));
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
    }

    private void deleteSFTPFile(String path) throws IOException, JSchException, SftpException {
        List<Object> properties = parseFTPPath(path, 22);
        if (properties != null) {
            String username = (String) properties.get(0);
            String password = (String) properties.get(1);
            String server = (String) properties.get(2);
            Integer port = (Integer) properties.get(3);
            String remoteFile = (String) properties.get(4);
            remoteFile = (!remoteFile.startsWith("/") ? "/" : "") + remoteFile;

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
                channelSftp.rm(remoteFile);
            } finally {
                if(channelSftp != null)
                    channelSftp.exit();
                if(channel != null)
                    channel.disconnect();
                if(session != null)
                    session.disconnect();
            }
        }
    }

    private List<Object> parseFTPPath(String path, Integer defaultPort) {
        /*sftp|ftp://username:password@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("s?ftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.groupCount() == 4;
            Integer port = noPort || connectionStringMatcher.group(4) == null ? defaultPort : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remoteFile = connectionStringMatcher.group(noPort ? 4 : 5);
            return Arrays.asList((Object) username, password, server, port, remoteFile);
        } else return null;
    }

    private void copyJDBCToFile(String query, File file) throws SQLException {
        /*jdbc://connectionString;query*/
        Pattern queryPattern = Pattern.compile("(jdbc:[^@]*)@(.*)");
        Matcher queryMatcher = queryPattern.matcher(query);
        if (queryMatcher.matches()) {
            Connection conn = null;

            try {
                String connectionString = queryMatcher.group(1);
                String jdbcQuery = queryMatcher.group(2);
                conn = DriverManager.getConnection(connectionString);

                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery(jdbcQuery);

                    FileUtils.writeByteArrayToFile(file, BaseUtils.serializeResultSet(rs));

                } finally {
                    if (statement != null)
                        statement.close();
                }

            } catch (SQLException | IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (conn != null)
                    conn.close();
            }

        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect jdbc url. Please use format: connectionString@query"));
        }
    }

    private void copyMDBToFile(String path, File file) throws IOException {
        /*mdb://path:table;where*/
        Pattern queryPattern = Pattern.compile("mdb:\\/\\/(.*):([^;]*)(?:;(.*))?");
        Matcher queryMatcher = queryPattern.matcher(path);
        if (queryMatcher.matches()) {
            Database db = null;

            try {
                db = DatabaseBuilder.open(new File(queryMatcher.group(1)));

                Table table = db.getTable(queryMatcher.group(2));

                String field = null;
                String sign = null;
                String value = null;

                boolean isWhere = false;
                String where = queryMatcher.group(3);
                if(where != null) {
                    Pattern wherePattern = Pattern.compile("([^=<>]*)([=<>]*)([^=<>]*)");
                    Matcher whereMatcher = wherePattern.matcher(where);
                    isWhere = whereMatcher.matches();
                    if (isWhere) {
                        field = whereMatcher.group(1);
                        sign = whereMatcher.group(2);
                        value = whereMatcher.group(3);
                    }
                }

                List<Map<String, Object>> rows = new ArrayList<>();

                for (Row rowEntry : table) {

                    boolean ignoreRow = false;
                    if(isWhere) {
                        if(!rowEntry.containsKey(field)) {
                            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. No such column. Note: names are sensitive"));
                        }
                        Object fieldValue = rowEntry.get(field);
                        if (fieldValue == null)
                            ignoreRow = true;
                        else if (fieldValue instanceof Integer) {
                            if (ignoreRowIntegerCondition(fieldValue, sign, value))
                                ignoreRow = true;
                        } else if (fieldValue instanceof java.util.Date) {
                            if (ignoreRowDateCondition(fieldValue, sign, value))
                                ignoreRow = true;
                        } else {
                            if (ignoreRowStringCondition(fieldValue, sign, value))
                                ignoreRow = true;
                        }
                    }
                    if(!ignoreRow) {
                        Map<String, Object> row = new HashMap<>();
                        for (Map.Entry<String, Object> entry : rowEntry.entrySet()) {
                            row.put(entry.getKey(), entry.getValue());
                        }
                        rows.add(row);
                    }
                }

                FileUtils.writeByteArrayToFile(file, BaseUtils.serializeCustomObject(rows));

            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (db != null)
                    db.close();
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect mdb url. Please use format: mdb://path:table;where"));
        }
    }

    private boolean ignoreRowIntegerCondition(Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        Integer intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (Exception e) {
            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid value"));
        }
        switch (sign) {
            case "=":
                if (!fieldValue.equals(intValue))
                    ignoreRow = true;
                break;
            case ">=":
                if (((Integer) fieldValue).compareTo(intValue) < 0)
                    ignoreRow = true;
                break;
            case ">":
                if (((Integer) fieldValue).compareTo(intValue) <= 0)
                    ignoreRow = true;
                break;
            case "<=":
                if (((Integer) fieldValue).compareTo(intValue) > 0)
                    ignoreRow = true;
                break;
            case "<":
                if (((Integer) fieldValue).compareTo(intValue) >= 0)
                    ignoreRow = true;
                break;
        }
        return ignoreRow;
    }

    private boolean ignoreRowDateCondition(Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        java.util.Date dateValue;
        try {
            dateValue = DateUtils.parseDate(value, new String[] {"yyyy-MM-dd"});
        } catch (Exception e) {
            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid value"));
        }
        switch (sign) {
            case "=":
                if (((java.util.Date) fieldValue).compareTo(dateValue) != 0)
                    ignoreRow = true;
                break;
            case ">=":
                if (((java.util.Date) fieldValue).compareTo(dateValue) < 0)
                    ignoreRow = true;
                break;
            case ">":
                if (((java.util.Date) fieldValue).compareTo(dateValue) <= 0)
                    ignoreRow = true;
                break;
            case "<=":
                if (((java.util.Date) fieldValue).compareTo(dateValue) > 0)
                    ignoreRow = true;
                break;
            case "<":
                if (((java.util.Date) fieldValue).compareTo(dateValue) >= 0)
                    ignoreRow = true;
                break;
        }
        return ignoreRow;
    }

    private boolean ignoreRowStringCondition(Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        String stringFieldValue = String.valueOf(fieldValue);
        switch (sign) {
            case "=":
                if (!stringFieldValue.equals(value))
                    ignoreRow = true;
                break;
            case ">=":
                if (stringFieldValue.compareTo(value) < 0)
                    ignoreRow = true;
                break;
            case ">":
                if (stringFieldValue.compareTo(value) <= 0)
                    ignoreRow = true;
                break;
            case "<=":
                if (stringFieldValue.compareTo(value) > 0)
                    ignoreRow = true;
                break;
            case "<":
                if (stringFieldValue.compareTo(value) >= 0)
                    ignoreRow = true;
                break;
        }
        return ignoreRow;
    }
}
