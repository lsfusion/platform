package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

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

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getIntegerType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getIntegerSQL();
    }

    public Integer read(Object value) {
        if(value==null) return null;
        return ((Number)value).intValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
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
    public Number getInfiniteValue(boolean min) {
        return min ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }
}
