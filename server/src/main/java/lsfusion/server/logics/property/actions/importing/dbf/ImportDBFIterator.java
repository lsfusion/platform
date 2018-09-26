package lsfusion.server.logics.property.actions.importing.dbf;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfFieldTypeEnum;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;

public class ImportDBFIterator extends ImportIterator {
    private static final String EQ = "=";
    private static final String GE = ">=";
    private static final String GT = ">";
    private static final String LE = "<=";
    private static final String LT = "<";
    private static final String IN = " IN ";
    private CustomDbfReader reader;
    private List<Integer> sourceColumns;
    private List<List<String>> wheresList;
    private final ImOrderSet<LCP> properties;
    private File tempMemoFile;
    private String charset;
    
    public ImportDBFIterator(CustomDbfReader reader, List<Integer> sourceColumns, List<List<String>> wheresList, ImOrderSet<LCP> properties, File tempMemoFile, String charset) {
        this.reader = reader;
        this.sourceColumns = sourceColumns;
        this.wheresList = wheresList;
        this.properties = properties;
        this.tempMemoFile = tempMemoFile;
        this.charset = charset;
    }

    @Override
    public Object nextRow() {
        try {
            CustomDbfRecord record = reader.read();
            if(record == null)
                return null;
            Map<Integer, String> fieldMapping = new HashMap<>();
            int i = 1;
            for(DbfField field : record.getFields()) {
                fieldMapping.put(i, field.getName());
                i++;
            }

            List<String> listRow = new ArrayList<>();
            if (!record.isDeleted() && !ignoreRow(record, wheresList)) {
                for (Integer column : sourceColumns) {
                    ValueClass valueClass = properties.get(sourceColumns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                    String field = fieldMapping.get(column);
                    if(field != null) {
                        if (valueClass instanceof DateClass) {
                            Date date = record.getDate(field);
                            listRow.add(date == null ? null : DateClass.getDateFormat().format(date));
                        } else if (valueClass instanceof DateTimeClass) {
                            Date dateTime = record.getDate(field);
                            listRow.add(dateTime == null ? null : DateTimeClass.getDateTimeFormat().format(dateTime));
                        } else if (valueClass instanceof LogicalClass) {
                            Boolean bool = record.getBoolean(field);
                            listRow.add(bool == null ? null : String.valueOf(bool));
                        } else {
                            DbfFieldTypeEnum fieldType = record.getField(field).getType();
                            if (fieldType == DbfFieldTypeEnum.Double) {
                                Double d = record.getDouble(field);
                                listRow.add(d == null ? null : String.valueOf(d));
                            } else if (fieldType == DbfFieldTypeEnum.Memo) {
                                listRow.add(record.getMemoAsString(field, Charset.forName(charset)));
                            } else listRow.add(record.getString(field, charset));
                        }
                    } else {
                        listRow.add(null);
                    }
                }
                return listRow;
            } else return "ignored"; //чтобы отличить пропускаемый ряд от null - конца чтения

        } catch (IOException | ParseException e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean ignoreRow(CustomDbfRecord record, List<List<String>> wheresList) {
        boolean ignoreRow = false;
        for (List<String> where : wheresList) {
            String condition = where.get(0);
            boolean and = condition != null && condition.equals("AND");
            boolean or = condition != null && condition.equals("OR");
            boolean not = where.get(1) != null;
            String field = where.get(2) != null ? where.get(2).toUpperCase() : null;
            String sign = where.get(3);
            String value = where.get(4);

            if (record.getField(field) == null) {
                throw new RuntimeException("Incorrect WHERE in IMPORT DBF: no such column");
            }
            Object fieldValue = record.getString(field, charset);
            boolean conditionResult = fieldValue == null || ignoreRowStringCondition(not, fieldValue, sign, value);
            ignoreRow = and ? (ignoreRow | conditionResult) : or ? (ignoreRow & conditionResult) : conditionResult;
        }
        return ignoreRow;
    }

    @Override
    protected void release() {
        try {
            if (reader != null)
                reader.close();
            if(tempMemoFile != null && !tempMemoFile.delete())
                tempMemoFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean ignoreRowStringCondition(boolean not, Object fieldValue, String sign, String value) {
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

    private List<String> splitIn(String value) {
        List<String> values = null;
        if (value.matches("\\(.*\\)")) {
            try {
                values = Arrays.asList(value.substring(1, value.length() - 1).split(","));
            } catch (Exception ignored) {
            }
            if (values == null)
                throw new RuntimeException("Incorrect WHERE in IMPORT. Invalid \"IN\" condition");
        }
        return values;
    }
}
