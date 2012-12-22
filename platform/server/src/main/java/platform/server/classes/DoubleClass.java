package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;
import platform.server.logics.ServerResourceBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DoubleClass extends IntegralClass<Double> {

    public final static DoubleClass instance = new DoubleClass();

    static {
        DataClass.storeClass(instance);
    }

    protected DoubleClass() { super(ServerResourceBundle.getString("classes.real")); }

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

    public String getDB(SQLSyntax syntax) {
        return syntax.getDoubleType();
    }
    public int getSQL(SQLSyntax syntax) {
        return syntax.getDoubleSQL();
    }

    public Double read(Object value) {
        if(value==null) return null;
        return ((Number) value).doubleValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
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
    public Number getInfiniteValue() {
        return Double.POSITIVE_INFINITY;
    }
}
