package lsfusion.server.classes;

import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import lsfusion.base.DateConverter;
import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

public class DateTimeClass extends DataClass<Timestamp> {

    public final static DateTimeClass instance = new DateTimeClass();

    static {
        DataClass.storeClass(instance);
    }

    private DateTimeClass() { super(ServerResourceBundle.getString("classes.time")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.date.with.time");
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

        reportField.alignment = HorizontalAlignEnum.RIGHT.getValue();
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

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDateTimeType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getDateTimeSQL();
    }

    public Timestamp read(Object value) {
        if (value == null) return null;
        return (Timestamp) value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setTimestamp(num, (Timestamp) value);
    }

    @Override
    public boolean isSafeType(Object value) {
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
        throw new RuntimeException("not supported");
    }

    public Timestamp parseString(String s) throws ParseException {
        try {
            return DateConverter.dateToStamp(getDateTimeFormat().parse(s));
        } catch (Exception e) {
            throw new ParseException("error parsing datetime", e);
        }
    }

    public static DateFormat getDateTimeFormat() {
        return new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    }

    public static String format(Date date) {
        return getDateTimeFormat().format(date);
    }

    public String getSID() {
        return "DateTimeClass";
    }

    @Override
    public Object getInfiniteValue(boolean min) {
        return min ? new Timestamp(0) : new Timestamp(Long.MAX_VALUE);
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
}
