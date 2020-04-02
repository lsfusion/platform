package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.DateConverter;
import lsfusion.base.SystemUtils;
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
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;

import static lsfusion.base.DateConverter.*;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.getWriteDate;

public class DateClass extends DataClass<LocalDate> {

    public final static DateClass instance = new DateClass();

    static {
        DataClass.storeClass(instance);
    }

    private DateClass() { super(LocalizedString.create("{classes.date}")); }

    public int getReportPreferredWidth() { return 70; }

    public Class getReportJavaClass() {
        return LocalDate.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalTextAlignEnum.RIGHT;
    }

    public byte getTypeID() {
        return DataType.DATE;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateClass?this:null;
    }

    public LocalDate getDefaultValue() {
        return getWriteDate(LocalDate.now());
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDateType();
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
        return syntax.getDateSQL();
    }

    public LocalDate read(Object value) {
        if(value instanceof LocalDate)
            return (LocalDate) value;
        else
            return sqlDateToLocalDate(safeDateToSql((Date) value));
    }

    @Override
    public LocalDate read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return sqlDateToLocalDate(set.getDate(name));
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDate(num, localDateToSqlDate(((LocalDate) value)));
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
        return "{d '" + value + "'}";
    }

    public static DateFormat getDateFormat() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        dateFormat.setTimeZone(SystemUtils.getCurrentTimeZone());  
        return dateFormat;
    }

    @Override
    public LocalDate parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws java.text.ParseException {
        return readDBF(dbfRecord.getDate(fieldName));
    }
    @Override
    public LocalDate parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        LocalDate cellValue;
        try {
            cellValue = sqlDateToLocalDate(safeDateToSql(cell.getDateCellValue()));//in apache.poi 4.1: cell.getLocalDateTimeCellValue().toLocalDate();
        } catch (IllegalStateException e) {
            return super.parseXLS(cell, formulaValue); // if cell can not parse date, we'll try to parse it as a string
        }
        return readXLS(cellValue);
    }

    public LocalDate parseString(String s) throws ParseException {
        try {
            //try to parse with default locale formats
            for(FormatStyle formatStyle : new FormatStyle[]{FormatStyle.MEDIUM, FormatStyle.SHORT}) {
                try {
                    return LocalDate.parse(s, DateTimeFormatter.ofLocalizedDate(formatStyle));
                } catch (Exception ignored) {
                }
            }
            LocalDateTime result = DateConverter.smartParse(s);
            return result != null ? result.toLocalDate() : null;
        } catch (Exception e) {
            throw new ParseException("error parsing date: " + s, e);
        }
    }

    public String formatString(LocalDate value) {
        return value == null ? null : getDateFormat().format(localDateToSqlDate(value));
    }

    public String getSID() {
        return "DATE";
    }

    @Override
    public LocalDate getInfiniteValue(boolean min) {
        return min ? LocalDate.MIN : LocalDate.MAX;
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'D', 8, 0);
    }

    @Override
    public void formatXLS(LocalDate object, Cell cell, ExportXLSWriter.Styles styles) {
        if (object != null) {
            cell.setCellValue(localDateToSqlDate(object)); //no need to convert in apache.poi 4.1
        }
        cell.setCellStyle(styles.date);
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }

    @Override
    public boolean useIndexedJoin() {
        return true;
    }
}
