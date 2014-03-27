package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

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

    @Override
    protected boolean isNegative(Long value) {
        return value < 0;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getLongType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getLongSQL();
    }

    public Long read(Object value) {
        if(value==null) return null;
        return ((Number)value).longValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
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
        return "LONG";
    }

    @Override
    public Number getInfiniteValue(boolean min) {
        return min ? Long.MIN_VALUE : Long.MAX_VALUE;
    }

    @Override
    public Stat getTypeStat() {
        return new Stat(Long.MAX_VALUE);
    }
}
