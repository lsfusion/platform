package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.ExtraReadInterface;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.DateConverter.safeDateToSql;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.sqlDateToLocalDate;

@Deprecated
public class ExtraReadProcessor implements ExtraReadInterface {

    private static final String EQ = "=";
    private static final String GE = ">=";
    private static final String GT = ">";
    private static final String LE = "<=";
    private static final String LT = "<";
    private static final String IN = " IN ";

    public ExtraReadProcessor() {
    }

    @Override
    public void copyToFile(String type, String query, File file) throws SQLException, IOException {
        switch (type) {
            case "jdbc":
                copyJDBCToFile(query, file);
                break;
            case "mdb":
                copyMDBToFile(query, file);
                break;
            default:
                throw new RuntimeException(String.format("READ %s is not supported", type));
        }
    }

    private void copyJDBCToFile(String query, File file) throws SQLException {
        //jdbc://connectionString@query
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
            throw new RuntimeException("Incorrect jdbc url. Please use format: connectionString@query");
        }
    }

    private void copyMDBToFile(String path, File file) throws IOException {
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
                            throw new RuntimeException("Incorrect WHERE in mdb url. No such column. Note: names are case sensitive");
                        }
                        boolean conditionResult;
                        Object fieldValue = rowEntry.get(field);
                        if (fieldValue == null)
                            conditionResult = true;
                        else if (fieldValue instanceof Integer) {
                            conditionResult = ignoreRowIntegerCondition(not, fieldValue, sign, value);
                        } else if (fieldValue instanceof Double) {
                            conditionResult = ignoreRowDoubleCondition(not, fieldValue, sign, value);
                        } else if (fieldValue instanceof java.util.Date) {
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
            ignoreRow = !dateValues.contains(fieldValue);
        } else {
            throw new RuntimeException("Incorrect mdb url. Please use format: mdb://path:table;where [NOT] condition1 [AND|OR conditionN]");
        }
        return not != ignoreRow;
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
            throw new RuntimeException("Incorrect WHERE in mdb url. Invalid value");
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
            throw new RuntimeException("Incorrect WHERE in mdb url. Invalid value");
        }
    }

    private static int compare(Object field1, Object field2) {
        if (field1 instanceof LocalDate) {
            return ((LocalDate) field1).compareTo((LocalDate) field2);
        } else if (field1 instanceof LocalTime) {
            return ((LocalTime) field1).compareTo((LocalTime) field2);
        } else if (field1 instanceof LocalDateTime) {
            return ((LocalDateTime) field1).compareTo((LocalDateTime) field2);
        } else return 0;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new RuntimeException("Incorrect WHERE in mdb url. Invalid value");
        }
    }

    private void copyMDBToFile(String path, File file) throws IOException {
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
                        } else if (fieldValue instanceof java.util.Date) {
                            conditionResult = ignoreRowDateCondition(not, sqlDateToLocalDate(safeDateToSql((Date) fieldValue)), sign, value);
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
                throw new RuntimeException("Incorrect WHERE in mdb url. Invalid \"IN\" condition");
        }
        return values;
    }
}
