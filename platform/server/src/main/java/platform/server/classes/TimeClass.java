package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

public class TimeClass extends DataClass<Time> {
    public final static TimeClass instance = new TimeClass();

    static {
        DataClass.storeClass(instance);
    }

    public TimeClass() {
        super(ServerResourceBundle.getString("classes.time"));
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof TimeClass ? this : null;
    }

    public byte getTypeID() {
        return Data.TIME;
    }

    protected Class getReportJavaClass() {
        return Time.class;
    }

    public Format getReportFormat() {
        return DateFormat.getTimeInstance(DateFormat.SHORT);
    }

    public SimpleDateFormat getDefaultFormat() {
        return new SimpleDateFormat("HH:mm:ss");
    }

    public Time parseString(String s) throws ParseException {
        try {
            return (Time) getDefaultFormat().parseObject(s);
        } catch (Exception e) {
            throw new ParseException("error parsing time", e);
        }
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getTimeType();
    }

    public int getSQL(SQLSyntax syntax) {
        return syntax.getTimeSQL();
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    @Override
    public int getBinaryLength(boolean charBinary) {
        return (charBinary ? 1 : 2) * 25;
    }

    @Override
    public boolean isSafeType(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setTime(num, (Time) value);
    }

    public String getSID() {
        return "TimeClass";
    }

    public Object getDefaultValue() {
        return new Time(System.currentTimeMillis());
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }

    public Time read(Object value) {
        return value == null ? null : (Time) value;
    }
}
