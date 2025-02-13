package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.TimeConverter;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

import static lsfusion.base.TimeConverter.localTimeToSqlTime;
import static lsfusion.base.TimeConverter.sqlTimeToLocalTime;

public class TimeClass extends HasTimeClass<LocalTime> {

    private TimeClass(LocalizedString caption, ExtInt millisLength) {
        super(caption, millisLength);
    }

    private final static Collection<TimeClass> timeClasses = new ArrayList<>();
    public final static TimeClass instance = get(ExtInt.UNLIMITED);
    public static TimeClass get(ExtInt millisLength) {
        return getCached(timeClasses, millisLength, () -> new TimeClass(LocalizedString.create("{classes.time}"), millisLength));
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof TimeClass ? this : null;
    }

    public byte getTypeID() {
        return DataType.TIME;
    }

    protected Class getReportJavaClass() {
        return java.time.LocalTime.class;
    }

    @Override
    public String getDefaultPattern() {
        LocalePreferences localePreferences = ThreadLocalContext.get().getLocalePreferences();
        return localePreferences != null ? localePreferences.timeFormat : ThreadLocalContext.getTFormats().timePattern;
    }

    @Override
    protected LocalTime parseFormat(String s, DateTimeFormatter formatter) throws ParseException {
        try {
            try {
                return LocalTime.parse(s, formatter);
            } catch (Exception ignored) {
            }
            return TimeConverter.smartParse(s);
        } catch (Exception e) {
            throw ParseException.propagateWithMessage("Error parsing time: " + s, e);
        }
    }

    @Override
    public LocalTime parseString(String s) throws ParseException {
        return parseFormat(s, Settings.get().isUseISOTimeFormatsInIntegration() ? DateTimeFormatter.ISO_LOCAL_TIME : ThreadLocalContext.getTFormats().timeParser);
    }

    @Override
    public String formatString(LocalTime value) {
        return value != null ? value.format(Settings.get().isUseISOTimeFormatsInIntegration() ? DateTimeFormatter.ISO_LOCAL_TIME : ThreadLocalContext.getTFormats().timeFormatter) : null;
    }

    public String getDBString(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getTimeType(millisLength);
    }

    public String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public String getDotNetRead(String reader) {
        throw new UnsupportedOperationException();
    }

    public String getDotNetWrite(String writer, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBaseDotNetSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalTime parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        Time cellValue;
        try {
            cellValue = new Time(cell.getDateCellValue().getTime());
        } catch (IllegalStateException e) {
            return super.parseXLS(cell, formulaValue);
        }
        return readXLS(cellValue);
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getTimeSQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(25);
    }

    @Override
    public boolean isSafeType() {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setTime(num, localTimeToSqlTime((LocalTime) value));
    }

    public String getSID() {
        return "TIME";
    }

    public LocalTime getDefaultValue() {
        return LocalTime.now();
    }

    @Override
    public LocalTime getInfiniteValue(boolean min) {
        return min ? LocalTime.MIN : LocalTime.MAX;
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
    
    public LocalTime read(Object value) {
        if(value instanceof LocalTime)
            return (LocalTime) value;
        else
            return sqlTimeToLocalTime((Time) value);
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return OverJDBField.createField(fieldName, 'C', 8, 0);
    }

    @Override
    public void formatXLS(LocalTime object, Cell cell, ExportXLSWriter.Styles styles) {
        if (object != null) {
            cell.setCellValue(localTimeToSqlTime(object));
        }
        cell.setCellStyle(styles.time);
    }

    @Override
    public LocalTime read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return super.read(set, syntax, name); //return set.getTime(name); в частности jtds глючит String вместо Time возвращает
    }

    @Override
    public String getIntervalProperty() {
        return "interval[TIME,TIME]";
    }

    @Override
    public String getFromIntervalProperty() {
        return "from[INTERVAL[TIME]]";
    }

    @Override
    public String getToIntervalProperty() {
        return "to[INTERVAL[TIME]]";
    }
}
