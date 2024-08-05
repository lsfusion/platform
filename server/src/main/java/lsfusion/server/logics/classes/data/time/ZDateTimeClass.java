package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.DateConverter;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.connection.LocalePreferences;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;

import static lsfusion.base.DateConverter.*;

public class ZDateTimeClass extends HasTimeClass<Instant> {

    private ZDateTimeClass(LocalizedString caption, ExtInt millisLength) {
        super(caption, millisLength);
    }

    private final static Collection<ZDateTimeClass> zDateTimeClasses = new ArrayList<>();
    public final static ZDateTimeClass instance = get(ExtInt.UNLIMITED);
    public static ZDateTimeClass get(ExtInt millisLength) {
        return getCached(zDateTimeClasses, millisLength, () -> new ZDateTimeClass(LocalizedString.create("{classes.date.with.time.with.zone}"), millisLength));
    }

    public int getReportPreferredWidth() {
        return 80;
    }

    public Class getReportJavaClass() {
        return java.time.Instant.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
        reportField.pattern = ThreadLocalContext.getTFormats().zDateTimePattern;
    }

    public byte getTypeID() {
        return DataType.ZDATETIME;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof ZDateTimeClass ? this : null;
    }

    public Instant getDefaultValue() {
        return Instant.now();
    }

    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getZDateTimeType(millisLength);
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
        return syntax.getZDateTimeSQL();
    }

    public Instant read(Object value) {
        return (Instant) value;
    }

    @Override
    public Instant read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return sqlTimestampToInstant(set.getTimestamp(name));
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setTimestamp(num, instantToSqlTimestamp((Instant) value));
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
    public String getValueAlignmentHorz() {
        return "end";
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public Instant parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws java.text.ParseException {
        return readDBF(dbfRecord.getDate(fieldName));
    }

    @Override
    public Instant parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        Instant cellValue;
        try {
            cellValue = sqlTimestampToInstant(new Timestamp(cell.getDateCellValue().getTime()));
        } catch (IllegalStateException e) {
            return super.parseXLS(cell, formulaValue);
        }
        return readXLS(cellValue);
    }

    public Instant parseString(String s) throws ParseException {
        try {
            try {
                return ZonedDateTime.parse(s, ThreadLocalContext.getTFormats().zDateTimeParser).toInstant();
            } catch (DateTimeParseException ignored) {
            }
            return DateConverter.smartParseInstant(s);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing zdatetime: " + s, e);
        }
    }

    @Override
    public String formatString(Instant value, boolean ui) {
        LocalePreferences localePreferences = ThreadLocalContext.get().getLocalePreferences();
        return value != null ? (ui && localePreferences != null ?
                value.atZone(TimeZone.getTimeZone(localePreferences.timeZone).toZoneId()).format(DateTimeFormatter.ofPattern(localePreferences.dateTimeFormat)) :
                ThreadLocalContext.getTFormats().zDateTimeFormatter.format(value)) : null;
    }

    public String getSID() {
        return "ZDATETIME";
    }

    @Override
    public Instant getInfiniteValue(boolean min) {
        return min ? DateTimeClass.minDate.toInstant(ZoneOffset.UTC) : DateTimeClass.maxDate.toInstant(ZoneOffset.UTC);
    }

    // actually is used only for OrderClass.getSource
    @Override
    public String getString(Object value, SQLSyntax syntax) {
        assert value != null;
        Instant instant = (Instant) value;
        return "to_timestamp(" + instant.getEpochSecond() + ", 0.0)";
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
    public void formatXLS(Instant object, Cell cell, ExportXLSWriter.Styles styles) {
        if(object != null) {
            cell.setCellValue(instantToSqlTimestamp(object));
        }
        cell.setCellStyle(styles.dateTime);
    }

    @Override
    public String getIntervalProperty() {
        return "interval[ZDATETIME,ZDATETIME]";
    }

    @Override
    public String getFromIntervalProperty() {
        return "from[INTERVAL[ZDATETIME]]";
    }

    @Override
    public String getToIntervalProperty() {
        return "to[INTERVAL[ZDATETIME]]";
    }
}

