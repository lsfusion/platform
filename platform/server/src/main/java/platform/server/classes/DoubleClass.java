package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DoubleClass extends IntegralClass<Double> {

    public final static DoubleClass instance = new DoubleClass();

    public String toString() {
        return "Плавающее число";
    }

    public Class getJavaClass() {
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
}
