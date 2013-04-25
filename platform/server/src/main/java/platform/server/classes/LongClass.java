package platform.server.classes;

import platform.interop.Data;
import platform.server.data.expr.query.Stat;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;

public class LongClass extends IntegralClass<Long> {

    public final static LongClass instance = new LongClass();

    static {
        DataClass.storeClass(instance);
    }

    private LongClass() { super(ServerResourceBundle.getString("classes.long.integer")); }

    public int getPreferredWidth() { return 65; }

    public String toString() {
        return ServerResourceBundle.getString("classes.big.integer");
    }

    public Class getReportJavaClass() {
        return Long.class;
    }

    public byte getTypeID() {
        return Data.LONG;
    }

    int getWhole() {
        return 10;
    }

    int getPrecision() {
        return 0;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getLongType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getLongSQL();
    }

    public Long read(Object value) {
        if(value==null) return null;
        return ((Number)value).longValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setLong(num, (Long)value);
    }

    public Object getDefaultValue() {
        return 0l;
    }

    public Format getReportFormat() {
        return NumberFormat.getIntegerInstance();
    }

    public Long parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            throw new ParseException("error parsing long", e);
        }
    }

    public String getSID() {
        return "LongClass";
    }

    @Override
    public Number getInfiniteValue() {
        return Math.round((double)(Long.MAX_VALUE / 2));
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
}
