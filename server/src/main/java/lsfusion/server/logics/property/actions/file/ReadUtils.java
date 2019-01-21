package lsfusion.server.logics.property.actions.file;

import com.google.common.base.Throwables;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.jcraft.jsch.*;
import lsfusion.base.*;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.JDBCTable;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.jfree.ui.ExtensionFileFilter;

import javax.swing.*;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadUtils {

    private static final String EQ = "=";
    private static final String GE = ">=";
    private static final String GT = ">";
    private static final String LE = "<=";
    private static final String LT = "<";
    private static final String IN = " IN ";

    public static ReadResult readFile(String sourcePath, boolean isDynamicFormatFileClass, boolean isBlockingFileRead, boolean isDialog) throws IOException, SftpException, JSchException, SQLException {
        if (isDialog) {
            sourcePath = showReadFileDialog(sourcePath);
            if(sourcePath == null) {
                return null;
            }
        }

        Path filePath = Path.parsePath(sourcePath, true);
        File file = null;
        String extension = null;
        switch (filePath.type) {
            case "file":
                file = new File(filePath.path);
                extension = BaseUtils.getFileExtension(file);
                break;
            case "http":
            case "https":
                file = File.createTempFile("downloaded", ".tmp");
                copyHTTPToFile(filePath, file);
                extension = BaseUtils.getFileExtension(new File(filePath.path));
                break;
            case "ftp":
                file = File.createTempFile("downloaded", ".tmp");
                copyFTPToFile(filePath.path, file);
                extension = BaseUtils.getFileExtension(new File(filePath.path));
                break;
            case "sftp":
                file = File.createTempFile("downloaded", ".tmp");
                copySFTPToFile(filePath.path, file);
                extension = BaseUtils.getFileExtension(new File(filePath.path));
                break;
            case "jdbc":
                file = File.createTempFile("downloaded", ".tmp");
                extension = "jdbc";
                copyJDBCToFile(filePath.path, file);
                break;
            case "mdb":
                file = File.createTempFile("downloaded", ".tmp");
                copyMDBToFile(filePath.path, file);
                extension = "mdb";
                break;
        }
        Object fileBytes; // RawFileData or FileData
        if (file != null && file.exists()) {
            if (isBlockingFileRead) {
                try (FileChannel channel = new RandomAccessFile(file, "rw").getChannel()) {
                    try (java.nio.channels.FileLock lock = channel.lock()) {
                        if (isDynamicFormatFileClass) {
                            fileBytes = new FileData(readBytesFromChannel(channel), extension);
                        } else {
                            fileBytes = readBytesFromChannel(channel);
                        }
                    }
                }
            } else {
                if (isDynamicFormatFileClass) {
                    fileBytes = new FileData(new RawFileData(file), extension);
                } else {
                    fileBytes = new RawFileData(file);
                }
            }
        } else {
            throw new RuntimeException("Read Error. File not found: " + sourcePath);
        }
        return new ReadResult(fileBytes, filePath.type, file.getAbsolutePath());
    }

    public static String showReadFileDialog(String path) {
        String result = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);

        File parentDir = null;
        if (path != null) {
            File file = new File(path.startsWith("file://") ? path.substring(7) : path);
            fileChooser.setSelectedFile(file);
            parentDir = file.getParentFile();
            String extension = BaseUtils.getFileExtension(file);
            if (!BaseUtils.isRedundantString(extension)) {
                ExtensionFileFilter filter = new ExtensionFileFilter("." + extension, extension);
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        fileChooser.setCurrentDirectory(parentDir != null ? parentDir : SystemUtils.loadCurrentDirectory());

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            result = fileChooser.getSelectedFile().getAbsolutePath();
            SystemUtils.saveCurrentDirectory(new File(result.substring(0, result.lastIndexOf("\\"))));
        }
        return result;
    }

    private static void copyHTTPToFile(Path path, File file) throws IOException {
        final List<String> properties = parseHTTPPath(path.path);
        if (properties != null) {
            final String username = properties.get(1);
            final String password = properties.get(2);
            String pathToFile = properties.get(3);
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(username, password.toCharArray()));
                }
            });
            URL httpUrl = new URL(URIUtil.encodeQuery(path.type + "://" + pathToFile));
            FileUtils.copyInputStreamToFile(httpUrl.openConnection().getInputStream(), file);
        } else {
            FileUtils.copyURLToFile(new URL(path.type + "://" + path.path), file);
        }
    }

    private static List<String> parseHTTPPath(String path) {
        /*http|https://username:password@path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("(.*):(.*)@(.*)");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String type = connectionStringMatcher.group(1);
            String username = connectionStringMatcher.group(2);
            String password = connectionStringMatcher.group(3);
            String pathToFile = connectionStringMatcher.group(4);
            return Arrays.asList(type, username, password, pathToFile);
        } else return null;
    }

    private static void copyFTPToFile(String path, File file) throws IOException {
        FTPPath properties = FTPPath.parseFTPPath(path, 21);
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
        if (properties.charset != null)
            ftpClient.setControlEncoding(properties.charset);
        try {

            ftpClient.connect(properties.server, properties.port);
            ftpClient.login(properties.username, properties.password);
            if(properties.passiveMode) {
                ftpClient.enterLocalPassiveMode();
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            OutputStream outputStream = new FileOutputStream(file);
            boolean done = ftpClient.retrieveFile(properties.remoteFile, outputStream);
            outputStream.close();
            if (!done) {
                throw Throwables.propagate(new RuntimeException("Some error occurred while downloading file from ftp"));
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (FTPConnectionClosedException e1) {
                ServerLoggers.importLogger.error("Ignored: ", e1);
            }
        }
    }

    private static void copySFTPToFile(String path, File file) throws JSchException, SftpException {
        FTPPath properties = FTPPath.parseFTPPath(path, 22);
        String remoteFile = properties.remoteFile;
        remoteFile = (!remoteFile.startsWith("/") ? "/" : "") + remoteFile;

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(properties.username, properties.server, properties.port);
            session.setPassword(properties.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            channelSftp.get(remoteFile, file.getAbsolutePath());
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    private static void copyJDBCToFile(String query, File file) throws SQLException {
        /*jdbc://connectionString@query*/
        Pattern queryPattern = Pattern.compile("([^@]*)@(.*)");
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

                    JDBCTable.serialize(rs).write(file);

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

    private static void copyMDBToFile(String path, File file) throws IOException {
        /*mdb://path:table;where [NOT] condition1 [AND|OR conditionN]*/
        /*conditions: field=value (<,>,<=,>=) or field IN (value1,value2,value3)*/
        Pattern queryPattern = Pattern.compile("(.*):([^;]*)(?:;([^;]*))*");
        Matcher queryMatcher = queryPattern.matcher(path);
        if (queryMatcher.matches()) {
            Database db = null;

            try {
                db = DatabaseBuilder.open(new File(queryMatcher.group(1)));

                Table table = db.getTable(queryMatcher.group(2));

                List<List<String>> wheresList = new ArrayList<>();

                String wheres = queryMatcher.group(3);
                if (wheres != null) { //spaces in value are not permitted
                    Pattern wherePattern = Pattern.compile("(?:\\s(AND|OR)\\s)?(?:(NOT)\\s)?([^=<>\\s]+)(\\sIN\\s|=|<|>|<=|>=)([^=<>\\s]+)");
                    Matcher whereMatcher = wherePattern.matcher(wheres);
                    while (whereMatcher.find()) {
                        String condition = whereMatcher.group(1);
                        String not = whereMatcher.group(2);
                        String field = whereMatcher.group(3);
                        String sign = whereMatcher.group(4);
                        String value = whereMatcher.group(5);
                        wheresList.add(Arrays.asList(condition, not, field, sign, value));
                    }
                }

                List<Map<String, Object>> rows = new ArrayList<>();

                for (Row rowEntry : table) {

                    boolean ignoreRow = false;

                    for (List<String> where : wheresList) {
                        String condition = where.get(0);
                        boolean and = condition != null && condition.equals("AND");
                        boolean or = condition != null && condition.equals("OR");
                        boolean not = where.get(1) != null;
                        String field = where.get(2);
                        String sign = where.get(3);
                        String value = where.get(4);

                        if (!rowEntry.containsKey(field)) {
                            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. No such column. Note: names are case sensitive"));
                        }
                        boolean conditionResult;
                        Object fieldValue = rowEntry.get(field);
                        if (fieldValue == null)
                            conditionResult = true;
                        else if (fieldValue instanceof Integer) {
                            conditionResult = ignoreRowIntegerCondition(not, fieldValue, sign, value);
                        } else if (fieldValue instanceof Double) {
                            conditionResult = ignoreRowDoubleCondition(not, fieldValue, sign, value);
                        } else if (fieldValue instanceof Date) {
                            conditionResult = ignoreRowDateCondition(not, fieldValue, sign, value);
                        } else {
                            conditionResult = ignoreRowStringCondition(not, fieldValue, sign, value);
                        }
                        ignoreRow = and ? (ignoreRow | conditionResult) : or ? (ignoreRow & conditionResult) : conditionResult;
                    }

                    if (!ignoreRow) {
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
            throw Throwables.propagate(new RuntimeException("Incorrect mdb url. Please use format: mdb://path:table;where [NOT] condition1 [AND|OR conditionN]"));
        }
    }

    private static boolean ignoreRowIntegerCondition(boolean not, Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<Integer> intValues = new ArrayList<>();
            for (String v : splitIn(value)) {
                intValues.add(parseInt(v));
            }
            ignoreRow = !intValues.contains(fieldValue);
        } else {
            Integer intValue = parseInt(value);
            switch (sign) {
                case EQ:
                    if (!fieldValue.equals(intValue))
                        ignoreRow = true;
                    break;
                case GE:
                    if (((Integer) fieldValue).compareTo(intValue) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (((Integer) fieldValue).compareTo(intValue) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (((Integer) fieldValue).compareTo(intValue) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (((Integer) fieldValue).compareTo(intValue) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid value"));
        }
    }

    private static boolean ignoreRowDoubleCondition(boolean not, Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<Double> doubleValues = new ArrayList<>();
            for (String v : splitIn(value)) {
                doubleValues.add(parseDouble(v));
            }
            ignoreRow = !doubleValues.contains(fieldValue);
        } else {

            Double doubleValue = parseDouble(value);
            switch (sign) {
                case EQ:
                    if (!fieldValue.equals(doubleValue))
                        ignoreRow = true;
                    break;
                case GE:
                    if (((Double) fieldValue).compareTo(doubleValue) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (((Double) fieldValue).compareTo(doubleValue) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (((Double) fieldValue).compareTo(doubleValue) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (((Double) fieldValue).compareTo(doubleValue) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid value"));
        }
    }

    private static boolean ignoreRowDateCondition(boolean not, Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<Date> dateValues = new ArrayList<>();
            for (String v : splitIn(value)) {
                dateValues.add(parseDate(v));
            }
            ignoreRow = !dateValues.contains(fieldValue);
        } else {

            Date dateValue = parseDate(value);
            switch (sign) {
                case EQ:
                    if (((Date) fieldValue).compareTo(dateValue) != 0)
                        ignoreRow = true;
                    break;
                case GE:
                    if (((Date) fieldValue).compareTo(dateValue) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (((Date) fieldValue).compareTo(dateValue) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (((Date) fieldValue).compareTo(dateValue) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (((Date) fieldValue).compareTo(dateValue) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private static Date parseDate(String value) {
        try {
            return DateUtils.parseDate(value, "yyyy-MM-dd");
        } catch (Exception e) {
            throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid value"));
        }
    }

    private static boolean ignoreRowStringCondition(boolean not, Object fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<String> stringValues = splitIn(value);
            ignoreRow = !stringValues.contains(fieldValue);
        } else {
            String stringFieldValue = String.valueOf(fieldValue);
            switch (sign) {
                case EQ:
                    if (!stringFieldValue.equals(value))
                        ignoreRow = true;
                    break;
                case GE:
                    if (stringFieldValue.compareTo(value) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (stringFieldValue.compareTo(value) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (stringFieldValue.compareTo(value) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (stringFieldValue.compareTo(value) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private static List<String> splitIn(String value) {
        List<String> values = null;
        if (value.matches("\\(.*\\)")) {
            try {
                values = Arrays.asList(value.substring(1, value.length() - 1).split(","));
            } catch (Exception ignored) {
            }
            if (values == null)
                throw Throwables.propagate(new RuntimeException("Incorrect WHERE in mdb url. Invalid \"IN\" condition"));
        }
        return values;
    }

    private static RawFileData readBytesFromChannel(FileChannel channel) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(IOUtils.BUFFER_SIZE);
        int left = Integer.MAX_VALUE;
        int readCount;
        while (left > 0 && (readCount = channel.read(buffer)) > 0) {
            out.write(buffer.array(), 0, readCount);
            left -= readCount;
        }
        return new RawFileData(out);
    }

    public static class ReadResult implements Serializable {
        Object fileBytes; // RawFileData or FileData
        String type;
        public String filePath;

        public ReadResult(Object fileBytes, String type, String filePath) {
            this.fileBytes = fileBytes;
            this.type = type;
            this.filePath = filePath;
        }
    }
}