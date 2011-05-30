package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.form.view.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

public class DateTimeClass extends DataClass<Timestamp> {

    public final static DateTimeClass instance = new DateTimeClass();
    private final static String sid = "DateTimeClass";

    static {
        DataClass.storeClass(sid, instance);
    }

    protected DateTimeClass() {
        super("Время");
    }

    public String toString() {
        return "Дата со временем";
    }

    public int getPreferredWidth() {
        return 75;
    }

    public Format getReportFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
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
        return Data.DATETIME;
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof DateTimeClass ? this : null;
    }

    public Object getDefaultValue() {
        return new Timestamp(System.currentTimeMillis());
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getDateTimeType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getDateTimeSQL();
    }

    public Timestamp read(Object value) {
        if (value == null) return null;
        return (Timestamp) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setTimestamp(num, (Timestamp) value);
    }

    @Override
    public boolean isSafeType(Object value) {
        return false;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return (charBinary ? 1 : 2) * 25;
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public Object parseString(String s) throws ParseException {
        try {
            return new SimpleDateFormat().parse(s);
        } catch (Exception e) {
            throw new ParseException("error parsing date", e);
        }
    }

    public String getSID() {
        return sid;
    }
}
