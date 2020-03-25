package lsfusion.server.logics.classes.data.time;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.DateConverter;
import lsfusion.base.SystemUtils;
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
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.time.LocalDate;

import static lsfusion.base.DateConverter.*;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.getWriteDate;

public class DateClass extends DataClass<Date> {

    public final static DateClass instance = new DateClass();

    static {
        DataClass.storeClass(instance);
    }

    private DateClass() { super(LocalizedString.create("{classes.date}")); }

    public int getReportPreferredWidth() { return 70; }

    public Class getReportJavaClass() {
        return java.util.Date.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
    }

    public byte getTypeID() {
        return DataType.DATE;
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DateClass?this:null;
    }

    public Date getDefaultValue() {
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

    public Date read(Object value) {
        DateConverter.assertDateToSql((java.util.Date)value);
        return safeDateToSql((java.util.Date)value);
    }

    @Override
    public Date read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return set.getDate(name);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDate(num, (Date)value);
    }

    @Override
    public boolean isSafeType() {
        return false;
    }

    @Override
    public ExtInt getCharLength() {
        return new ExtInt(25);
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
    public Date parseDBF(CustomDbfRecord dbfRecord, String fieldName, String charset) throws java.text.ParseException {
        return readDBF(dbfRecord.getDate(fieldName));
    }
    @Override
    public Date parseXLS(Cell cell, CellValue formulaValue) throws ParseException {
        java.util.Date cellValue;
        try {
            cellValue = cell.getDateCellValue();
        } catch (IllegalStateException e) {
            return super.parseXLS(cell, formulaValue); // if cell can not parse date, we'll try to parse it as a string
        }
        return readXLS(cellValue);
    }

    public Date parseString(String s) throws ParseException {
        try {
            java.util.Date parse;
            try {
                parse = getDateFormat().parse(s);
                DateConverter.assertDateToSql(parse);
            } catch (java.text.ParseException e) {
                parse = DateConverter.smartParse(s);
            }
            return safeDateToSql(parse);
        } catch (Exception e) {
            throw new ParseException("error parsing date: " + s, e);
        }
    }

    public String formatString(Date value) {
        return value == null ? null : getDateFormat().format(value);
    }

    public String getSID() {
        return "DATE";
    }

    @Override
    public Object getInfiniteValue(boolean min) {
        return DateConverter.dateToSql(new java.util.Date(min ? 0 : Long.MAX_VALUE));
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'D', 8, 0);
    }

    @Override
    public void formatXLS(Date object, Cell cell, ExportXLSWriter.Styles styles) {
        if (object != null) {
            cell.setCellValue(object);
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
