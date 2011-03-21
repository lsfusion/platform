package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class DateClass extends DataClass<Date> {

    public final static DateClass instance = new DateClass();
    private final static String sid = "DateClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected DateClass() {}

    public String toString() {
        return "Дата";
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

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
        return true;
    }

    public byte getTypeID() {
        return Data.DATE;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof DateClass?this:null;
    }

    public Object getDefaultValue() {
        return new Date(System.currentTimeMillis());
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getDateType();
    }

    public Date read(Object value) {
        if(value==null) return null;
        return (Date)value;
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
//        return "'" + value + "'";
        throw new RuntimeException("not supported");
    }

    public Object parseString(String s) throws ParseException {
        try {
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
            dateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
            return dateFormat.parse(s);
        } catch (Exception e) {
            throw new ParseException("error parsing date", e);
        }
    }

    public String getSID() {
        return sid;
    }
}
