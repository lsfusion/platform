package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

import lsfusion.server.physics.admin.Settings;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static lsfusion.base.DateConverter.*;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;

public class JDBField {

    private final String name;
    private final char type;
    private final int length;
    private final int decimalCount;

    public static JDBField createField(String name, char type, int i, int j) {
        try {
            return new JDBField(name, type, i, j);
        } catch (JDBFException e) {
            //default constructor throws exception without field name
            throw new RuntimeException(String.format("Creation of field '%s' failed: " + e.getMessage(), name), e);
        }
    }

    // the validation is the same as in the replaced com.hexiong.jdbf.JDBField
    public JDBField(String s, char c, int i, int j) throws JDBFException {
        if (s.length() > 10)
            throw new JDBFException("The field name is more than 10 characters long: " + s);
        if (c != 'C' && c != 'N' && c != 'L' && c != 'D' && c != 'F' && c != 'M')
            throw new JDBFException("The field type is not a valid. Got: " + c);
        if (i < 1)
            throw new JDBFException("The field length should be a positive integer. Got: " + i);
        if (c == 'C' && i >= 254)
            throw new JDBFException("The field length should be less than 254 characters for character fields. Got: " + i);
        if (c == 'N' && i >= 21)
            throw new JDBFException("The field length should be less than 21 digits for numeric fields. Got: " + i);
        if (c == 'L' && i != 1)
            throw new JDBFException("The field length should be 1 characater for logical fields. Got: " + i);
        if (c == 'D' && i != 8)
            throw new JDBFException("The field length should be 8 characaters for date fields. Got: " + i);
        if (c == 'F' && i >= 21)
            throw new JDBFException("The field length should be less than 21 digits for floating point fields. Got: " + i);
        if (j < 0)
            throw new JDBFException("The field decimal count should not be a negative integer. Got: " + j);
        if ((c == 'C' || c == 'L' || c == 'D') && j != 0)
            throw new JDBFException("The field decimal count should be 0 for character, logical, and date fields. Got: " + j);
        if (j > i - 1)
            throw new JDBFException("The field decimal count should be less than the length - 1. Got: " + j);

        this.name = s;
        this.type = c;
        this.length = i;
        this.decimalCount = j;
    }

    public String getName() {
        return name;
    }

    public char getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public int getDecimalCount() {
        return decimalCount;
    }

    public String format(Object obj) throws JDBFException {
        if (type == 'N' || type == 'F') {
            if (obj == null) {
                //obj = new Double(0.0D);
                String result = "";
                while (result.length() <= getLength())
                    result += " ";
                return result;
            }
            if (obj instanceof Number) {
                Number number = (Number) obj;
                StringBuilder stringBuilder = new StringBuilder(getLength());
                for (int i = 0; i < getLength(); i++) {
                    stringBuilder.append("#");

                }
                if (getDecimalCount() > 0) {
                    stringBuilder.setCharAt(getLength() - getDecimalCount() - 1, '.');

                    if (Settings.get().isExportDBFNumericMandatoryZeroes()) {
                        stringBuilder.setCharAt(getLength() - getDecimalCount() - 2, '0');
                        for (int i = getLength() - getDecimalCount(); i < getLength(); i++) {
                            stringBuilder.setCharAt(i, '0');
                        }
                    }
                }
                DecimalFormat decimalformat = new DecimalFormat(stringBuilder.toString());
                DecimalFormatSymbols dfSymbols = decimalformat.getDecimalFormatSymbols();
                dfSymbols.setDecimalSeparator('.');
                decimalformat.setDecimalFormatSymbols(dfSymbols);

                String s1 = decimalformat.format(number);
                int k = getLength() - s1.length();
                if (k < 0) {
                    throw new JDBFException("Value " + number + " cannot fit in pattern: '" + stringBuilder + "'.");
                }
                StringBuilder stringBuilder2 = new StringBuilder(k);
                for (int l = 0; l < k; l++) {
                    stringBuilder2.append(" ");

                }
                return stringBuilder2 + s1;
            } else {
                throw new JDBFException("Expected a Number, got " + obj.getClass() + ".");
            }
        } else if (type == 'C') {
            if (obj == null) {
                obj = "";
            }
            if (obj instanceof LocalTime) {
                // the DBF format has no time type, so TIME is written as 'HH:mm:ss' text
                // (going through the string branch below for the length check and the padding)
                obj = new SimpleDateFormat("HH:mm:ss").format(localTimeToSqlTime((LocalTime) obj));
            }
            if (obj instanceof String) {
                String s = (String) obj;
                if (s.length() > getLength()) {
                    throw new JDBFException("'" + obj + "' is longer than " + getLength() + " characters.");
                }
                StringBuilder stringBuilder1 = new StringBuilder(getLength() - s.length());
                for (int j = 0; j < getLength() - s.length(); j++) {
                    stringBuilder1.append(' ');

                }
                return s + stringBuilder1;
            } else {
                throw new JDBFException("Expected a String, got " + obj.getClass() + ".");
            }
        } else if (type == 'L') {
            if (obj == null) {
                //obj = new Boolean(false);
                return " ";
            }
            if (obj instanceof Boolean) {
                Boolean boolean1 = (Boolean) obj;
                return boolean1 ? "T" : "F";
            } else {
                throw new JDBFException("Expected a Boolean, got " + obj.getClass() + ".");
            }
        } else if (type == 'D') {
            if (obj == null) {
                //obj = new Date();
                String result = "";
                while (result.length() <= 8)
                    result += " ";
                return result;
            }
            if (obj instanceof Date) {
                Date date = (Date) obj;
                return new SimpleDateFormat("yyyyMMdd").format(date);
            } else if (obj instanceof LocalDate) {
                Date date = localDateToSqlDate((LocalDate) obj);
                return new SimpleDateFormat("yyyyMMdd").format(date);
            } else if (obj instanceof LocalDateTime) {
                Date date = localDateTimeToSqlTimestamp((LocalDateTime) obj);
                return new SimpleDateFormat("yyyyMMdd").format(date);
            } else if (obj instanceof LocalTime) {
                Date date = localTimeToSqlTime((LocalTime) obj);
                return new SimpleDateFormat("yyyyMMdd").format(date);
            } else {
                throw new JDBFException("Expected a Date, got " + obj.getClass() + ".");
            }
        } else {
            throw new JDBFException("Unrecognized JDBField type: " + type);
        }
    }
}
