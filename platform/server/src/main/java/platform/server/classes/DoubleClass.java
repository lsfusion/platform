package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DoubleClass extends IntegralClass<Double> {

    public final static DoubleClass instance = new DoubleClass();
    private final static String sid = "DoubleClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected DoubleClass() {}

    public String toString() {
        return "Плавающее число";
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

    public Object parseString(String s) throws ParseException {
        try {
            return Double.parseDouble(s.replace(',','.'));
        } catch (Exception e) {
            return 0.0;
//            throw new ParseException("error parsing double", e);
        }
    }

    public String getSID() {
        return sid;
    }
}
