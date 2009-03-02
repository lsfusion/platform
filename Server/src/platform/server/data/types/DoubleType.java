package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.math.BigDecimal;

class DoubleType extends IntegralType<Double> {

    DoubleType() {
        super("D");
    }
    DoubleType(String iID) {
        super(iID);
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getDoubleType();
    }

    Object getMinValue() {
        return Double.MIN_VALUE;
    }

    public Double read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).doubleValue();
        else
        if(value instanceof Float)
            return ((Float) value).doubleValue();
        else
        if(value instanceof Long)
            return ((Long) value).doubleValue();
        if(value instanceof Integer)
            return ((Integer) value).doubleValue();
        else
            return (Double) value;
    }

    public boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}
