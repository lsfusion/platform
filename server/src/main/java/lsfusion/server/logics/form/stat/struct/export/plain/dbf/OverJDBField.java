package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

import com.hexiong.jdbf.JDBFException;
import com.hexiong.jdbf.JDBField;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static lsfusion.server.logics.classes.data.time.DateTimeConverter.localDateTimeToSqlTimestamp;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.localDateToSqlDate;

public class OverJDBField extends JDBField {

    private char type = super.getType();

    public OverJDBField(String s, char c, int i, int j) throws JDBFException {
        super(s, c, i, j);
    }

    @Override
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
                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
                return simpledateformat.format(date);
            } else if (obj instanceof LocalDate) {
                Date date = localDateToSqlDate((LocalDate) obj);
                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
                return simpledateformat.format(date);
            } else if (obj instanceof LocalDateTime) {
                Date date = localDateTimeToSqlTimestamp((LocalDateTime) obj);
                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
                return simpledateformat.format(date);
            } else {
                throw new JDBFException("Expected a Date, got " + obj.getClass() + ".");
            }
        } else {
            throw new JDBFException("Unrecognized JDBFField type: " + type);
        }
    }
}