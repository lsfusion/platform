package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.DateConverter;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;

import static lsfusion.base.DateConverter.*;

public class DateTimeClass extends TimeSeriesClass<LocalDateTime> {

    private DateTimeClass(LocalizedString caption, ExtInt millisLength) {
        super(caption, millisLength);
    }

    private final static Collection<DateTimeClass> dateTimeClasses = new ArrayList<>();
    public final static DateTimeClass dateTime = get(ExtInt.UNLIMITED);
    public static DateTimeClass get(ExtInt millisLength) {
        return getCached(dateTimeClasses, millisLength, () -> new DateTimeClass(LocalizedString.create("{classes.date.with.time.with.zone}"), millisLength));
    }

    public int getReportPreferredWidth() {
        return 80;
    }

    public Class getReportJavaClass() {
        return java.time.LocalDateTime.class;
    }

    @Override
    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
        reportField.pattern = ThreadLocalContext.getTFormats().dateTimePattern; // dateTimePattern;
    }

    public byte getTypeID() {
        return DataType.DATETIME;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateTimeClass ? this : null;
    }

    public LocalDateTime getDefaultValue() {
        return LocalDateTime.now();
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDateTimeType(millisLength);
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
            try {
                return LocalDateTime.parse(s, ThreadLocalContext.getTFormats().dateTimeParser);
            } catch (DateTimeParseException ignored) {
            }
            return DateConverter.smartParse(s);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing datetime: " + s, e);
        }
    }

    @Override
    public String formatString(LocalDateTime value) {
        return value == null ? null : value.format(ThreadLocalContext.getTFormats().dateTimeFormatter);
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
        return OverJDBField.createField(fieldName, 'D', 8, 0);
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

    @Override
    public String getIntervalProperty() {
        return "interval[DATETIME,DATETIME]";
    }

    @Override
    public String getFromIntervalProperty() {
        return "from[INTERVAL[DATETIME]]";
    }

    @Override
    public String getToIntervalProperty() {
        return "to[INTERVAL[DATETIME]]";
    }
}
