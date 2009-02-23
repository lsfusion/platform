package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;

import java.math.BigDecimal;

public class IntegerType extends IntegralType<Integer> {

    IntegerType() {
        super("I");
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getIntegerType();
    }

    Object getMinValue() {
        return Integer.MIN_VALUE;
    }

    public Integer read(Object value) {
        if(value instanceof BigDecimal)
            return ((BigDecimal) value).intValue();
        else
        if(value instanceof Double)
            return ((Double) value).intValue();
        else
        if(value instanceof Float)
            return ((Float) value).intValue();
        else
        if(value instanceof Long)
            return ((Long) value).intValue();
        else
            return (Integer) value;
    }

    public boolean greater(Object value1, Object value2) {
        return read(value1)>read(value2);
    }
}
