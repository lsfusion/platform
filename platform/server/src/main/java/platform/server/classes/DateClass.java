package platform.server.classes;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import platform.base.DateConverter;
import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;
import platform.server.logics.ServerResourceBundle;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class DateClass extends DataClass<Date> {

    public final static DateClass instance = new DateClass();

    static {
        DataClass.storeClass(instance);
    }

    protected DateClass() { super(ServerResourceBundle.getString("classes.date")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.date");
    }

    public int getPreferredWidth() { return 70; }

    public Format getReportFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public Class getReportJavaClass() {
        return java.util.Date.class;
    }

    public boolean fillReportDrawField(ReportDrawField reportField) {
        if (!super.fillReportDrawField(reportField))
            return false;

        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
        return true;
    }

    public byte getTypeID() {
        return Data.DATE;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof DateClass?this:null;
    }

    public Object getDefaultValue() {
        return DateConverter.getCurrentDate();
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getDateType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getDateSQL();
    }

    public Date read(Object value) {
        assert DateConverter.dateToSql((java.util.Date)value).equals(new java.sql.Date(((java.util.Date)value).getTime()));
        return DateConverter.safeDateToSql((java.util.Date)value);
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setDate(num, (Date)value);
    }

    @Override
    public boolean isSafeType(Object value) {
        return false;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return (charBinary?1:2) * 25;
    }

    public boolean isSafeString(Object value) {
        return false;
    }
    
    public String getString(Object value, SQLSyntax syntax) {
        return "{d '" + value + "'}";
    }

    public static DateFormat getDateFormat() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        dateFormat.setTimeZone(Calendar.getInstance().getTimeZone());  // todo [dale]: Нужно брать таймзону из бизнес-логики
        return dateFormat;
    }

    public Date parseString(String s) throws ParseException {
        try {
            java.util.Date date = getDateFormat().parse(s);
            assert DateConverter.dateToSql(date).equals(new java.sql.Date(date.getTime()));
            return DateConverter.safeDateToSql(getDateFormat().parse(s));
        } catch (Exception e) {
            throw new ParseException("error parsing date", e);
        }
    }

    public static String format(Date date) {
        return getDateFormat().format(date);
    }

    public String getSID() {
        return "DateClass";
    }

    @Override
    public Object getInfiniteValue() {
        return DateConverter.dateToSql(new java.util.Date(Long.MAX_VALUE));
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
}
