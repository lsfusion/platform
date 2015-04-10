package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
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
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadActionProperty extends ScriptingActionProperty {
    private final LCP<?> targetProp;

    public ReadActionProperty(ScriptingLogicsModule LM, ValueClass valueClass, LCP<?> targetProp) {
        super(LM, valueClass);
        this.targetProp = targetProp;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof StringClass;

        String path = (String) value.object;
        try {
            if (path != null) {
                Pattern p = Pattern.compile("(file|ftp|http|jdbc|mdb):(?:\\/\\/)?(.*)");
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);

                    File file = null;
                    String extension = null;
                    if (type.equals("file")) {
                        file = new File(url);
                        extension = BaseUtils.getFileExtension(file);
                    } else if (type.equals("http")) {
                        file = File.createTempFile("downloaded", "tmp");
                        FileUtils.copyURLToFile(new URL(path), file);
                        extension = BaseUtils.getFileExtension(new File(url));
                    } else if (type.equals("ftp")) {
                        file = File.createTempFile("downloaded", "tmp");
                        copyFTPToFile(path, file);
                        extension = BaseUtils.getFileExtension(new File(url));
                    } else if (type.equals("jdbc")) {
                        file = File.createTempFile("downloaded", "tmp");
                        extension = "jdbc";
                        copyJDBCToFile(path, file);
                    } else if (type.equals("mdb")) {
                        file = File.createTempFile("downloaded", "tmp");
                        copyMDBToFile(path, file);
                        extension = "mdb";
                    }
                    if (file != null && file.exists()) {
                        targetProp.change(BaseUtils.mergeFileAndExtension(IOUtils.getFileBytes(file), extension.getBytes()), context);
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

            } catch (SQLException e) {
                throw Throwables.propagate(e);
            } catch (IOException e) {
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

                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

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
                        Map<String, Object> row = new HashMap<String, Object>();
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
        if (sign.equals("=")) {
            if (!fieldValue.equals(intValue))
                ignoreRow = true;
        } else if (sign.equals(">=")) {
            if (((Integer) fieldValue).compareTo(intValue) < 0)
                ignoreRow = true;
        } else if (sign.equals(">")) {
            if (((Integer) fieldValue).compareTo(intValue) <= 0)
                ignoreRow = true;
        } else if (sign.equals("<=")) {
            if (((Integer) fieldValue).compareTo(intValue) > 0)
                ignoreRow = true;
        } else if (sign.equals("<")) {
            if (((Integer) fieldValue).compareTo(intValue) >= 0)
                ignoreRow = true;
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
        if (sign.equals("=")) {
            if (((java.util.Date)fieldValue).compareTo(dateValue) != 0)
                ignoreRow = true;
        } else if (sign.equals(">=")) {
            if (((java.util.Date)fieldValue).compareTo(dateValue) < 0)
                ignoreRow = true;
        } else if (sign.equals(">")) {
            if (((java.util.Date)fieldValue).compareTo(dateValue) <= 0)
                ignoreRow = true;
        } else if (sign.equals("<=")) {
            if (((java.util.Date)fieldValue).compareTo(dateValue) > 0)
                ignoreRow = true;
        } else if (sign.equals("<")) {
            if (((java.util.Date)fieldValue).compareTo(dateValue) >= 0)
                ignoreRow = true;
        }
        return ignoreRow;
    }

    private boolean ignoreRowStringCondition(Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        String stringFieldValue = String.valueOf(fieldValue);
        if (sign.equals("=")) {
            if (!stringFieldValue.equals(value))
                ignoreRow = true;
        } else if (sign.equals(">=")) {
            if (stringFieldValue.compareTo(value) < 0)
                ignoreRow = true;
        } else if (sign.equals(">")) {
            if (stringFieldValue.compareTo(value) <= 0)
                ignoreRow = true;
        } else if (sign.equals("<=")) {
            if (stringFieldValue.compareTo(value) > 0)
                ignoreRow = true;
        } else if (sign.equals("<")) {
            if (stringFieldValue.compareTo(value) >= 0)
                ignoreRow = true;
        }
        return ignoreRow;
    }
}
