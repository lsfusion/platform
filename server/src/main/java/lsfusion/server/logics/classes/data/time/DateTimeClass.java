package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.DateConverter;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.print.design.ReportDrawField;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.CustomDbfRecord;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import static lsfusion.base.DateConverter.*;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.getWriteDateTime;

public class DateTimeClass extends DataClass<LocalDateTime> {

    public final static DateTimeClass instance = new DateTimeClass();

    static {
        DataClass.storeClass(instance);
    }

    private DateTimeClass() { super(LocalizedString.create("{classes.date.with.time}")); }

    public int getReportPreferredWidth() {
        return 75;
    }

    public Class getReportJavaClass() {
        return Timestamp.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
    }

    public byte getTypeID() {
        return DataType.DATETIME;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateTimeClass ? this : null;
    }

    public LocalDateTime getDefaultValue() {
        return getWriteDateTime(LocalDateTime.now());
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDateTimeType();
    }
    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return "SqlDateTime";
    }

    public String getDotNetRead(String reader) {
        return "DateTime.FromBinary("+reader+".ReadInt64())";
    }

    public String getDotNetWrite(String writer, String value) {
        return writer + ".Write(" +value + ".ToBinary());";
    }

    public int getBaseDotNetSize() {
        return 8;
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getDateTimeSQL();
    }

    public LocalDateTime read(Object value) {
        if(value instanceof LocalDateTime)
            return (LocalDateTime) value;
        else if (value instanceof java.util.Date) {
            return sqlTimestampToLocalDateTime(dateToStamp((java.util.Date) value));
        } else return null;
    }

    @Override
    public LocalDateTime read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return sqlTimestampToLocalDateTime(set.getTimestamp(name));
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setTimestamp(num, localDateTimeToSqlTimestamp((LocalDateTime) value));
    }

    @Override
    public boolean isSafeType() {
        return false;
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(25);
    }

    @Override
    public FlexAlignment getValueAlignment() {
        return FlexAlignment.END;
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    @Override
    public LocalDateTime parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws java.text.ParseException {
        return readDBF(dbfRecord.getDate(fieldName));
    }
    @Override
    public LocalDateTime parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        LocalDateTime cellValue;
        try {
            cellValue = cell.getLocalDateTimeCellValue();
        } catch (IllegalStateException e) {
            return super.parseXLS(cell, formulaValue);
        }
        return readXLS(cellValue);
    }

    public LocalDateTime parseString(String s) throws ParseException {
        try {
            //try to parse with default locale formats
            FormatStyle[] formatStyles = new FormatStyle[]{FormatStyle.SHORT, FormatStyle.MEDIUM};
            for(FormatStyle dateStyle : formatStyles) {
                for(FormatStyle timeStyle : formatStyles) {
                    try {
                        return LocalDateTime.parse(s, DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle));
                    } catch (DateTimeParseException ignored) {
                    }
                }
            }
            return DateConverter.smartParse(s);
        } catch (Exception e) {
            throw new ParseException("error parsing datetime: " + s, e);
        }
    }

    @Override
    public String formatString(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM));
    }

    public String getSID() {
        return "DATETIME";
    }

    @Override
    public LocalDateTime getInfiniteValue(boolean min) {
        return min ? LocalDateTime.MIN : LocalDateTime.MAX;
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'D', 8, 0);
    }

    @Override
    public void formatXLS(LocalDateTime object, Cell cell, ExportXLSWriter.Styles styles) {
        if(object != null) {
            cell.setCellValue(object);
        }
        cell.setCellStyle(styles.dateTime);
    }

    @Override
    public boolean useIndexedJoin() {
        return true;
    }
}
