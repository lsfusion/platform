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

public class IntegerClass extends IntegralClass<Integer> {

    public final static IntegerClass instance = new IntegerClass();

    static {
        DataClass.storeClass(instance);
    }

    protected IntegerClass() { super(ServerResourceBundle.getString("classes.integer")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.integer");
    }

    public Class getReportJavaClass() {
        return Integer.class;
    }

    public byte getTypeID() {
        return Data.INTEGER;
    }

    int getWhole() {
        return 8;
    }

    int getPrecision() {
        return 0;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntegerSQL();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setInt(num, (Integer)value);
    }

    public Object getDefaultValue() {
        return 0;
    }

    public Format getReportFormat() {
        return NumberFormat.getIntegerInstance();
    }

    public Integer parseString(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            throw new ParseException("error parsing int", e);
        }
    }

    public String getSID() {
        return "IntegerClass";
    }

    @Override
    public Number getInfiniteValue() {
        return Integer.MAX_VALUE / 2;
    }
}
