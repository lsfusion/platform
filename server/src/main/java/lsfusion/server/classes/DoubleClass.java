package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DoubleClass extends IntegralClass<Double> {

    public final static DoubleClass instance = new DoubleClass();

    static {
        DataClass.storeClass(instance);
    }

    private DoubleClass() { super(ServerResourceBundle.getString("classes.real")); }

    public String toString() {
        return ServerResourceBundle.getString("classes.floating");
    }

    public Class getReportJavaClass() {
        return Double.class;
    }

    public byte getTypeID() {
        return Data.DOUBLE;
    }

    int getWhole() {
        return 99999;
    }

    int getPrecision() {
        return 99999;
    }

    @Override
    protected boolean isNegative(Double value) {
        return value < 0.0;
    }

    public String getDB(SQLSyntax syntax, TypeEnvironment typeEnv) {
        return syntax.getDoubleType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getDoubleSQL();
    }

    public Double read(Object value) {
        if(value==null) return null;
        return ((Number) value).doubleValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax, TypeEnvironment typeEnv) throws SQLException {
        statement.setDouble(num, (Double)value);
    }

    public Object getDefaultValue() {
        return 0.0;
    }

    public Double parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(s.replace(',','.'));
        } catch (Exception e) {
            return 0.0;
//            throw new ParseException("error parsing double", e);
        }
    }

    public String getSID() {
        return "DoubleClass";
    }

    @Override
    public Number getInfiniteValue(boolean min) {
        return min ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }
}
