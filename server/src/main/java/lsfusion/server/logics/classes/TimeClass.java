package lsfusion.server.logics.classes;

import com.hexiong.jdbf.JDBFException;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.form.stat.integration.exporting.plain.dbf.OverJDBField;
import lsfusion.server.logics.form.stat.integration.exporting.plain.xls.ExportXLSWriter;
import org.apache.poi.ss.usermodel.Cell;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeClass extends DataClass<Time> {
    public final static TimeClass instance = new TimeClass();

    static {
        DataClass.storeClass(instance);
    }

    private TimeClass() { super(LocalizedString.create("{classes.time}")); }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof TimeClass ? this : null;
    }

    public byte getTypeID() {
        return DataType.TIME;
    }

    protected Class getReportJavaClass() {
        return Time.class;
    }

    public SimpleDateFormat getDefaultFormat() {
        return new SimpleDateFormat("HH:mm:ss");
    }

    public Time parseString(String s) throws ParseException {
        try {
            return new Time(((Date) getDefaultFormat().parseObject(s)).getTime());
        } catch (Exception e) {
            throw new ParseException("error parsing time", e);
        }
    }

    @Override
    public String formatString(Time value) {
        return value == null ? null : getTimeFormat().format(value);
    }

    public static DateFormat getTimeFormat() {
        return new SimpleDateFormat("HH:mm:ss");
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getTimeType();
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
        statement.setTime(num, (Time) value);
    }

    public String getSID() {
        return "TIME";
    }

    public Time getDefaultValue() {
        return new Time(System.currentTimeMillis());
    }

    @Override
    public Object getInfiniteValue(boolean min) {
        return min ? new Time(0, 0, 0) : new Time(23, 59, 59);
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
    
    public Time read(Object value) {
        if(value == null)
            return null;
        if(value instanceof String)
            return Time.valueOf(((String)value).substring(0, 8));
        return (Time) value;
    }

    @Override
    public OverJDBField formatDBF(String fieldName) throws JDBFException {
        return new OverJDBField(fieldName, 'C', 8, 0);
    }

    @Override
    public void formatXLS(Time object, Cell cell, ExportXLSWriter.Styles styles) {
        if (object != null) {
            cell.setCellValue(object);
        }
        cell.setCellStyle(styles.time);
    }

    @Override
    public Time read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        return super.read(set, syntax, name); //return set.getTime(name); в частности jtds глючит String вместо Time возвращает
    }
}
