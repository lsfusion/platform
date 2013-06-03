package lsfusion.server.classes;

import lsfusion.base.ExtInt;
import lsfusion.interop.Data;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeClass extends DataClass<Time> {
    public final static TimeClass instance = new TimeClass();

    static {
        DataClass.storeClass(instance);
    }

    private TimeClass() { super(ServerResourceBundle.getString("classes.time")); }

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
            return new Time(((Date) getDefaultFormat().parseObject(s)).getTime());
        } catch (Exception e) {
            throw new ParseException("error parsing time", e);
        }
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getTimeType();
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
    public boolean isSafeType(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        return "'" + value + "'";
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setTime(num, (Time) value);
    }

    public String getSID() {
        return "TimeClass";
    }

    public Object getDefaultValue() {
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
        return value == null ? null : (Time) value;
    }
}
